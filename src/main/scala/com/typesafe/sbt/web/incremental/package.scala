/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.sbt.web

import java.io.File

/**
 * The incremental task API lets tasks run more quickly when they are
 * called more than once. The idea is to do less work when tasks are
 * called a second time, by skipping any work that has already been done.
 * In other words, tasks only perform the “incremental” work that is
 * necessary since they were last run.
 * 
 * To analyse which work needs to be done, a task’s work is broken up
 * into a number of sub-operations, each of which can be run
 * independently. Each operation takes input parameters and can read and
 * write files. The incremental task API keeps a record of which
 * operations have been run so that that those operations don’t need to
 * be repeated in the future.
 * 
 * Here is how tasks interact with the API:
 * 
 * - Tasks call the API with a list of potential operations to perform and
 *   with a function to run operations.
 * 
 * - The API takes care of pruning the list of operations to find the
 *   incremental operations that need to be run.
 * 
 * - The API then calls the supplied function to run the pruned list of
 *   operations. This method returns a list of results, one for each
 *   operation.
 * 
 * - If an operation succeeds, the details are recorded so that the
 *   operation can be skipped in the future, if possible.
 * 
 * Behind the scenes, syncIncremental maintains a record of each operation
 * that succeeds. It uses these records to work out which operations need
 * to be run and which can be skipped.
 * 
 * Each operation is assumed to take some input parameters, optionally
 * read and write some files, and either succeed or fail when it runs. An
 * operation which fails will always be run again even if its parameters
 * and input files remain the same. But an operation which succeeds will
 * only need to be run again if its input parameters change or if the
 * contents of any files it read from or wrote to have changed.
 */
package object incremental {

  /**
   * Syncs operations that haven't been run before. This method is the main
   * interface to the incremental API.
   *
   * This method will delete products under the following conditions:
   *  * If the an op from a previous run doesn't exist in the current run,
   *    all of its products that weren't produced by other ops in the
   *    previous run or ops from the current run will be deleted.
   *  * If an op produced a product in a previous run, but didn't produce
   *    it in the current run, and no other op produced it, it will be
   *    deleted.
   *
   * @tparam Op The Op type parameter gives the type of the individual
   * operations. There are no restrictions on which type callers
   * should use here. The syncIncremental method treats Op as a
   * completely opaque type. The caller can use whatever abstract
   * representation of operations is most convenient; functions,
   * strings, custom classes, etc are all possible. The only
   * requirement is that the caller must provide two arguments to
   * allow syncIncremental to work with operations: runOps to run a
   * sequence of operations and return the operations’ results and
   * inputHasher to get a hash of an operation’s inputs.
   *
   * @tparam A The A type parameter gives the return type of the
   * syncIncremental method. The runOps method returns a value of type
   * A and this value is then returned by the syncIncremental method.
   *
   * @param cacheDirectory A parent directory for a cache file that
   *                       will be used for caching information between
   *                       invocations.
   *
   * @param ops The ops parameter is a list of possible operations to
   * perform. The syncIncremental method will prune this list and call
   * the run parameter with the pruned list.
   *
   * @param runOps The runOps function returns a (Map[Op,OpResult],A).
   * The Map[Op,OpResult] is used to update the cache of operations.
   * The A value is used by syncIncremental as its return value for
   * syncIncremental method.
   *
   * Each OpResult can be either an OpSuccess or an OpFailure. If an
   * operation succeeded, it should return the paths of any files it read
   * from or wrote to.
   *
   * Example OpResults:
   * - Read 1 file, no output file
   *   {{{OpSuccess(filesRead = Set(sourceFile), filesWritten = Set.empty)}}}
   * - Read 1 file, wrote 1 file
   *   {{{OpSuccess(filesRead = Set(sourceFile), filesWritten = Set(targetFile))}}}
   * - Read 3 files, wrote 2 files
   *   {{{OpSuccess(filesRead = Set(a, b, c), filesWritten = Set(d, e))}}}
   * - Failed with 1 problem
   *   {{{OpFailure}}}
   * - Failed with a problem, but without any details
   *   {{{OpFailure}}}
   * - An unexpected error which doesn’t need to be displayed to the user
   *   in a special way
   *   {{{throw new DecodeException(“Couldn’t compile: failed to decode ”)}}}
   *
   * @param inputHasher The inputHasher implicit parameter lets the syncIncremental
   * method distinguish between operations’ input parameters. In addition to
   * reading and writing files, operations usually take input
   * parameters, for example compilation settings. The incremental API
   * doesn’t need to know the content of these parameters, but it does
   * need to be able to tell if parameters are the same or different.
   * Callers must provide an implicit OpInputHasher so that the API can
   * distinguish different operations’ parameters.
   *
   * @return A tuple of all the output files, including those that were
   *         produced by a previous run and didn't need to be re run, and
   *         the result of the runOps method.
   */
  def syncIncremental[Op,A](
                            cacheDirectory: File, ops: Seq[Op])(
                            runOps: Seq[Op] => (Map[Op, OpResult], A))(
                            implicit inputHasher: OpInputHasher[Op]): (Set[File], A) = {
    // Load the cache from a file in the current task's cache directory
    val cacheFile: File = new File(cacheDirectory, "op-cache")
    val cache: OpCache = OpCacheIO.fromFile(cacheFile)

    // Before vacuuming the cache, find out what the old cache had produced
    val allOldProducts = cache.content.values.to(Set).flatMap(_.products)

    // Clear out any unknown operations from the existing cache
    OpCache.vacuumExcept(cache, ops)

    // Work out the minimal set of ops we need to run and run them
    val prunedOps: Seq[Op] = OpCache.newOrChanged(cache, ops)
    val (results: Map[Op, OpResult], finalResult) = runOps(prunedOps)

    // Check returned results are all within the set of given ops
    val prunedOpsSet: Set[Op] = prunedOps.to(Set)
    val resultOpsSet: Set[Op] = results.keySet
    val unexpectedOps: Set[Op] = resultOpsSet -- prunedOpsSet
    if (!unexpectedOps.isEmpty) {
      throw new IllegalArgumentException(s"runOps function returned results for unknown ops: $unexpectedOps")
    }

    // Work out what the current valid products are
    val opsSet: Set[Op] = ops.to(Set)
    val oldProductsToKeep = OpCache.productsForOps(cache, opsSet -- prunedOpsSet)
    val newProducts = results.values.to(Set).flatMap {
      case OpFailure => Set.empty[File]
      case OpSuccess(_, products) => products
    }
    val currentProducts = oldProductsToKeep ++ newProducts

    // Delete all products that weren't produced by previous non expired ops, or by the current run
    val productsToDelete = allOldProducts -- currentProducts
    productsToDelete.filterNot(_.isDirectory).foreach(_.delete())

    // Update the cache with the new information (vacuuming, new results)
    OpCache.cacheResults(cache, results)
    OpCacheIO.toFile(cache, cacheFile)

    (currentProducts, finalResult)
  }

  /**
   * A simple OpInputHasher, based on a hash of the Op's toString value. This
   * hasher can be used if the Op includes all relevant operation input
   * information in it's toString representation. If not, then another hasher
   * should be used, e.g.
   *
   * {{{
   * implicit val fileAndOptionsHasher = OpInputHasher[File]{ file =>
   *   OpInputHash.hashString(file.toString + “|” + options.toString))
   * }
   * }}}
   */
  implicit def toStringInputHasher[Op] = OpInputHasher[Op](op => OpInputHash.hashString(op.toString))

}