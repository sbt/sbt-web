package com.typesafe.sbt.web

import scalax.collection.Graph
import scala.collection.immutable
import sbt._
import scala.language.higherKinds
import scalax.collection.edge.LDiEdge
import java.security.MessageDigest

/**
 * The base node type of our graph. Represents things related to source files.
 */
sealed trait SourceNode

/**
 * The source file itself.
 */
case class SourceFile(f: File) extends SourceNode

/**
 * Source files are associated with a representation of build state.
 * This state equates to anything that, if changed, should cause the source file to
 * be seen as being a candidate for processing. This may include the file's modification time plus
 * a raft of build settings.
 *
 * The build stamp's value is delivered as a string and can be conveniently converted as a digest by using
 * the associated companion object. You would do this in order to economise on the size of a build stamp in
 * memory e.g. if part of the build stamp should be a digest of the source file's contents.
 */
case class BuildStamp(p: String) extends SourceNode

object BuildStamp {
  private val digester = MessageDigest.getInstance("MD5")

  def digest(s: String): String = digester.digest(s.getBytes).map("%02X".format(_)).mkString
}

/**
 * State that represents a directed graph of source files. Each edge is labelled such
 * that the graph can be used to represent multiple types of relationship e.g. there may be edges associated
 * with source file to source file relationships, and others for recording build stamps.
 */
class SourceFileGraph(val g: Graph[SourceNode, LDiEdge]) {
  /**
   * Conveniently return all source file predecessors of a given sequence of source files.
   */
  def modifiedPredecessors(modifiedSources: immutable.Seq[File]): immutable.Seq[File] = {
    modifiedSources.flatMap {
      f =>
        g.get(SourceFile(f)).diPredecessors
    }.flatMap {
      n =>
        n.value match {
          case sf: SourceFile => Some(sf.f)
          case _ => None
        }
    }
  }
}

/**
 * A functional state transformer for transforming directed source file graphs.
 * @param a the transformation logic.
 * @tparam A The product of transformation in addition to the state being transformed.
 */
class SourceFileGraphTransformer[A](a: SourceFileGraph => (SourceFileGraph, A)) {

  def apply(s: SourceFileGraph): (SourceFileGraph, A) = a(s)
}

/**
 * State that represents files which are potentially effected given a subset of changes
 * represented by a smaller graph. Any labels in the original graph that are not in the
 * smaller graph are carried forward automatically. This labelling of a graph permits
 * sections of the graph to be dealt with in isolation e.g. you could use this to apply
 * source file relationships for a given plugin's task by using a label equating to that.
 */
class AgedSourceFileGraph(
                           g: Graph[SourceNode, LDiEdge],
                           val ng: Graph[SourceNode, LDiEdge]
                           ) extends SourceFileGraph(g)

/**
 * ModifiedFiles is a state transformer that is able to to receive a graph of
 * files and produce a sequence of files representing those files that are candiates
 * for processing given some a change in the graph.
 */
object ModifiedFiles extends SourceFileGraphTransformer[immutable.Seq[File]]({
  case pfg: AgedSourceFileGraph =>

    val labels = pfg.ng.edges.map(e => e.label.toString).toSet

    val unmodifiedSources =
      pfg.g.filter(pfg.g.having(node = n => pfg.ng.contains(n))) ++
        pfg.g.edges.filterNot(e => labels.contains(e.label.toString))

    val newSources =
      pfg.ng.filterNot(
        pfg.ng.having(node = n => unmodifiedSources.contains(n))
      )

    val nextSources = unmodifiedSources ++ newSources

    val modifiedSourceFiles = newSources.edges.flatMap {
      e =>
        e._1.value match {
          case sf: SourceFile => Some(sf.f)
          case _ => None
        }
    }.to[immutable.Seq]

    (new SourceFileGraph(nextSources), modifiedSourceFiles)
})
