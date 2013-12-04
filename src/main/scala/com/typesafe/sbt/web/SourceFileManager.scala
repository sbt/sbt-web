package com.typesafe.sbt.web

import sbt.File
import scala.collection.immutable
import scalax.collection.Graph
import scalax.collection.edge.LDiEdge

// FIXME:   read the graph and persist it. Develop some tests also.

/**
 * A source file manager retains durable data across multiple sbt builds and a
 * simplified api to a SourceFileGraph.
 * @param cacheFile is where the file is retrieved from and persisted to.
 */
class SourceFileManager(cacheFile: File) {

  var sourceFileGraph = new SourceFileGraph(Graph.empty[SourceNode, LDiEdge])

  private def updateGraph(updateGraph: Graph[SourceNode, LDiEdge]): immutable.Seq[File] = {
    val result = ModifiedFiles(new AgedSourceFileGraph(sourceFileGraph.g, updateGraph))
    sourceFileGraph = result._1
    result._2
  }

  /**
   * Given a set of source file/build-stamp tuples, return those that are modified since last time.
   */
  def setAndCompareBuildStamps(sources: immutable.Seq[(File, String)]): immutable.Seq[File] = {
    updateGraph(Graph.from(
      Nil,
      sources.map {
        s => LDiEdge(
          SourceFile(s._1): SourceNode,
          BuildStamp(s._2)
        )("build-stamp")
      }
    ))
  }

  /**
   * Register a sequence of source file/input tuples where the input represents a
   * sequence of source files that the corresponding source file depends on. Return those modified
   * since last time.
   */
  def setAndCompareInputs(sources: immutable.Seq[(File, immutable.Seq[File])]): immutable.Seq[File] = {
    updateGraph(Graph.from(
      Nil,
      sources.flatMap {
        s =>
          s._2.map {
            in =>
              LDiEdge(
                SourceFile(s._1): SourceNode,
                SourceFile(in)
              )("input")
          }
      }
    ))
  }

  /**
   * Return the distinct inputs of a sequence of sources.
   */
  def inputs(sources: immutable.Seq[File]): Set[File] = {
    sourceFileGraph.predecessors(sources)
  }

  /**
   * Save the graph.
   */
  def save(): Unit = {

  }
}

object SourceFileManager {
  def apply(cacheFile: File) = new SourceFileManager(cacheFile)
}