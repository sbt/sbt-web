package com.typesafe.web.sbt

import sbt.{IO, File}
import scala.collection.immutable
import scalax.collection.Graph
import scalax.collection.edge.LDiEdge
import scala.pickling._
import binary._

/**
 * A source file manager retains durable data across multiple sbt builds and a
 * simplified api to a SourceFileGraph.
 * @param cacheFile is where the file is retrieved from and persisted to.
 */
class SourceFileManager(cacheFile: File) {

  var sourceFileGraph = if (cacheFile.exists()) {
    //FIXME:    IO.readBytes(cacheFile).unpickle[SourceFileGraph]
    new SourceFileGraph(Graph[SourceNode, LDiEdge]().empty)
  } else {
    new SourceFileGraph(Graph[SourceNode, LDiEdge]().empty)
  }

  /**
   * Given a set of source file/build-stamp tuples, return those that are modified since last time.
   */
  def setAndCompareBuildStamps(sources: immutable.Seq[(File, String)]): immutable.Seq[File] = {
    val result = ModifiedFiles(new AgedSourceFileGraph(sourceFileGraph.g, Graph.from(
      Nil,
      sources.map {
        s => LDiEdge(
          SourceFile(s._1): SourceNode,
          BuildStamp(s._2)
        )("build-stamp")
      }
    )))
    sourceFileGraph = result._1
    result._2
  }

  /**
   * Register a sequence of source file/input tuples where the input represents a
   * sequence of source files that the corresponding source file depends on.
   */
  def setInputs(inputs: immutable.Seq[(File, immutable.Seq[File])]): Unit = {
    sourceFileGraph = new SourceFileGraph(
      sourceFileGraph.g.filterNot(sourceFileGraph.g.having(edge = _.label == "input")) ++
        Graph.from(
          Nil,
          inputs.flatMap {
            s =>
              s._2.map {
                in =>
                  LDiEdge(
                    SourceFile(in): SourceNode,
                    SourceFile(s._1)
                  )("input")
              }
          }
        )
    )
  }

  /**
   * Return the distinct inputs of a sequence of inputs.
   */
  def inputs(sources: immutable.Seq[File]): Set[File] = {
    sourceFileGraph.predecessors(sources)
  }

  /**
   * Save the graph.
   */
  def save(): Unit = {
    // FIXME: IO.write(cacheFile, sourceFileGraph.pickle.value)
  }
}

object SourceFileManager {
  def apply(cacheFile: File) = new SourceFileManager(cacheFile)
}