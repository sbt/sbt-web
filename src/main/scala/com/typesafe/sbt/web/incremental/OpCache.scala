/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.sbt.web.incremental

import java.io.File
import sbt.Hash
import scala.collection.immutable.Set

/**
 * Cache for recording which operations have successfully completed. Associates
 * a hash of the operations' inputs (OpInputHash) with a record of the files
 * that were accessed by the operation.
 */
private[incremental] class OpCache(var content: Map[OpInputHash, OpCache.Record] = Map.empty) {
  import OpCache._
  def allOpInputHashes: Set[OpInputHash] = content.keySet
  def contains(oih: OpInputHash): Boolean = {
    content.contains(oih)
  }
  def getRecord(oih: OpInputHash): Option[Record] = {
    content.get(oih)
  }
  def putRecord(oih: OpInputHash, record: Record) = {
    content = content + ((oih, record))
  }
  def removeRecord(oih: OpInputHash) = {
    content = content - oih
  }
}

/**
 * Useful methods for working with an OpCache.
 */
private[incremental] object OpCache {

  /**
   * A record stored in a cache. At the moment a record stores
   * a list of files accessed, but the record could be extended to store other
   * information in the future.
   */
  final case class Record(fileHashes: Set[FileHash], products: Set[File])

  /**
   * A hash of a file's content.
   * @param sha1IfExists `Some(sha1)` if the file exists, `None` otherwise.
   */
  final case class FileHash(file: File, sha1IfExists: Option[Bytes])

  /**
   * Read a file and capture a hash of its file content.
   */
  def fileHash(file: File): FileHash = {
    val sha1IfExists = if (file.exists) {
      val bytes = Bytes(Hash(file))
      Some(bytes)
    } else None
    FileHash(file, sha1IfExists)
  }

  /**
   * Check if any of the given FileHash objects have changed.
   */
  def anyFileChanged(fileHashes: Set[FileHash]): Boolean = {
    fileHashes.foldLeft(false) ({
      case (true, _) => true // We've already found a changed file, no need to check other files
      case (false, recordedContent) => {
        val currentContent = OpCache.fileHash(recordedContent.file)
        val fileChanged = currentContent != recordedContent
        fileChanged
      }
    })
  }

  /**
   * Remove all operations from the cache that aren't in the given set of operations.
   */
  def vacuumExcept[Op](cache: OpCache, opsToKeep: Seq[Op])(implicit opInputHasher: OpInputHasher[Op]): Unit = {
    val oihSet: Set[OpInputHash] = opsToKeep.map(opInputHasher.hash).to(Set)
    for (oih <- cache.allOpInputHashes) yield {
      if (!oihSet.contains(oih)) {
        cache.removeRecord(oih)
      }
    }
  }

  /**
   * Given a set of operations, filter out any operations that are in the cache
   * and unchanged, and only return operations that are not in the cache or that
   * are in the cache but have changed.
   */
  def newOrChanged[Op](cache: OpCache, ops: Seq[Op])(implicit opInputHasher: OpInputHasher[Op]): Seq[Op] = {
    val opsAndHashes: Seq[(Op, OpInputHash)] = ops.map(w => (w, opInputHasher.hash(w)))
    opsAndHashes.filter {
      case (_, wh) =>
        cache.getRecord(wh).fold(true) { record =>
          // Check that cached file hashes are up to date
          val fileChanged = OpCache.anyFileChanged(record.fileHashes)
          if (fileChanged) cache.removeRecord(wh)
          fileChanged
        }
    } map {
      case (w, _) => w
    }
  }

  /**
   * Add the result of an operation into the cache.
   */
  def cacheResult(cache: OpCache, oih: OpInputHash, or: OpResult): Unit = or match {
    case OpFailure =>
      // Shouldn't actually be present in the cache, but clear just in case
      if (cache.contains(oih)) cache.removeRecord(oih)
    case OpSuccess(filesRead, filesWritten) =>
      val fileHashes: Set[FileHash] = (filesRead ++ filesWritten).map(OpCache.fileHash)
      val record = Record(fileHashes, filesWritten)
      cache.putRecord(oih, record)
  }
  /**
   * Add multiple operations and results into the cache.
   */
  def cacheResults[Op](cache: OpCache, results: Map[Op, OpResult])(implicit opInputHasher: OpInputHasher[Op]): Unit = {
    for ((op, or) <- results) {
      cacheResult(cache, opInputHasher.hash(op), or)
    }
  }

  /**
   * Get all the products for the given ops in the cache.
   */
  def productsForOps[Op](cache: OpCache, ops: Set[Op])(implicit opInputHasher: OpInputHasher[Op]): Set[File] = {
    ops.flatMap { op =>
      val record = cache.getRecord(opInputHasher.hash(op))
      record.fold(Set.empty[File])(_.products)
    }.toSet
  }
}