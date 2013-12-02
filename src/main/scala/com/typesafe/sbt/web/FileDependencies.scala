package com.typesafe.sbt.web

import scalax.collection.Graph
import scala.collection.immutable
import sbt._
import scala.language.higherKinds
import scalax.collection.edge.LDiEdge

/**
 * State that represents a directed graph of files.
 */
class FileGraph(val g: Graph[File, LDiEdge])

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
 * file system. The file mappings represent a pairing where the first member is the
 * source file, and the second member is an associated file.
 */
class PendingFileGraph(
                        val label: String,
                        g: Graph[File, LDiEdge],
                        val ng: Graph[File, LDiEdge]
                        ) extends FileGraph(g)

/**
 * Modified Files is a state transformer that is able to to receive a graph of
 * files and produce a sequence of files representing some delta between the graphs
 * transforming from and to.
 */
object ModifiedFiles extends FileGraphTransformer[immutable.Seq[File]]({
  case pfg: PendingFileGraph =>

    val unmodifiedSources =
      pfg.g.filter(pfg.g.having(node = n => pfg.ng.contains(n))) ++
        pfg.g.edges.filterNot(e => e.label == pfg.label)

    val newSources =
      pfg.ng.filterNot(
        pfg.ng.having(node = n => unmodifiedSources.contains(n))
      )

    val nextSources = unmodifiedSources ++ newSources

    val modifiedSourceFiles = newSources.edges.map(_._1.value).to[immutable.Seq]

    (new FileGraph(nextSources), modifiedSourceFiles)
})