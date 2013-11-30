package com.typesafe.sbt.web

import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge
import scala.collection.immutable
import sbt._
import scala.language.higherKinds

/**
 * State that represents a directed graph of files.
 */
class FileGraph(val g: Graph[File, DiEdge])

/**
 * A functional state transformer for transforming directed file graphs.
 * @param a the transformation logic.
 * @tparam A The product of transformation in addition to the state being transformed.
 */
class FileGraphTransformer[A](a: FileGraph => (FileGraph, A)) {

  def apply(s: FileGraph): (FileGraph, A) = a(s)

  def !(s: FileGraph): FileGraph = a(s)._1

  def ~>(s: FileGraph): A = a(s)._2
}

/**
 * State that represents files which are potentially effected given changes to the
 * file system.
 */
class PendingFileGraph(
                        g: Graph[File, DiEdge],
                        val unmanagedSources: Seq[File],
                        val jsFilter: FileFilter,
                        val copiedResources: Seq[(File, File)]
                        ) extends FileGraph(g)

/**
 * Modified Files is a state transformer that is able to to receive a graph of
 * files and produce a sequence of files representing some delta between the graphs
 * transforming from and to. That delta is expressed through a function during the
 * transformer's construction.
 */
object ModifiedFiles extends FileGraphTransformer[immutable.Seq[File]]({
  case s: PendingFileGraph =>

    // Build an index for convenient access to the source->target mapping.
    val copiedResourcesIndex = s.copiedResources.toMap

    // Build a graph of nodes representing js source files that have no associated target js files. This graph
    // represents all js sources files that we should be looking at.
    val unmanagedJsSources = Graph.from[File, DiEdge]((s.unmanagedSources ** s.jsFilter).get, Nil)

    // Build a graph of nodes representing js source files that we have not seen on previous runs.
    val newUnmanagedJsSources = unmanagedJsSources -- s.g

    // Conditionally eliminate nodes from the last time we processed them. Nodes representing the source js file
    // are checked to see whether they still exist as part of unmanaged sources.
    val existingLastJsSourceGraph =
      s.g.filter(
        s.g.having(edge = e => copiedResourcesIndex.contains(e._1))
      )

    // Build a graph of nodes that have actually been modified since we last processed.
    val modifiedLastJsSourceGraph =
      existingLastJsSourceGraph
        .nodes
        .flatMap(x => x.incoming)
        .filter(x => x._1.lastModified > x._2.lastModified)

    // Update the old graph with a union of the new unmanaged sources along with their target folder
    // information.
    val nextJsSourceGraph =
      existingLastJsSourceGraph ++
        Graph.from[File, DiEdge](
          newUnmanagedJsSources.nodes.map(n => n.value),
          newUnmanagedJsSources.nodes.map(n => DiEdge(n.value, copiedResourcesIndex.get(n.value).get))
        )

    // Determine the sequence of sources that have either been modified or are new to us.
    val modifiedJsSources =
      (modifiedLastJsSourceGraph.map(_._1) ++ newUnmanagedJsSources.nodes)
        .map(n => n.value)
        .to[immutable.Seq]

    // Return the updated graph along with a sequence of sources that were found to be modified.
    (new FileGraph(nextJsSourceGraph), modifiedJsSources)
})