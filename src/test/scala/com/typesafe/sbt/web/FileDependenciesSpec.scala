package com.typesafe.sbt.web

import sbt._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import scalax.collection.Graph
import scalax.collection.edge.Implicits._
import scalax.collection.edge.LDiEdge

@RunWith(classOf[JUnitRunner])
class FileDependenciesSpec extends Specification {

  "source dependencies" should {
    "return all new files as modified given an empty graph" in {
      val sourceFile = file("source")
      val sourceNode: SourceNode = SourceFile(sourceFile)
      val props = BuildStamp(BuildStamp.digest("someprops"))

      val (fg, modifiedSources) = ModifiedFiles(
        new AgedSourceFileGraph(
          Graph[SourceNode, LDiEdge](),
          Graph((sourceNode ~+> props)("xyz"))
        )
      )

      fg.g.order must_== 2
      fg.g.graphSize must_== 1
      fg.g.get(sourceNode).diSuccessors must_== Set(props)
      modifiedSources.size must_== 1
    }

    "return all existing files as unmodified given a previous graph" in {
      val sourceFile = file("source")
      val sourceNode: SourceNode = SourceFile(sourceFile)
      val props = BuildStamp("someprops")

      val (fg, modifiedSources) = ModifiedFiles(
        new AgedSourceFileGraph(
          Graph((sourceNode ~+> props)("xyz")),
          Graph((sourceNode ~+> props)("xyz"))
        )
      )

      fg.g.order must_== 2
      fg.g.graphSize must_== 1
      fg.g.get(sourceNode).diSuccessors must_== Set(props)
      modifiedSources.size must_== 0
    }

    "return one existing file as modified given a previous graph with a removed file" in {
      val sourceFile = file("/a/source")
      val sourceNode: SourceNode = SourceFile(sourceFile)
      val props = BuildStamp("someprops")

      val deletedSourceFile = file("/b/source")
      val deletedSourceNode: SourceNode = SourceFile(deletedSourceFile)
      val deletedProps = BuildStamp("somedeletedprops")

      val (fg, modifiedSources) = ModifiedFiles(
        new AgedSourceFileGraph(
          Graph((deletedSourceNode ~+> deletedProps)("xyz")),
          Graph((sourceNode ~+> props)("xyz"))
        )
      )

      fg.g.order must_== 2
      fg.g.graphSize must_== 1
      fg.g.get(sourceNode).diSuccessors must_== Set(props)
      modifiedSources.size must_== 1
    }

    "return one existing file as modified given a previous graph along with unrelated nodes given their labels" in {
      val sourceFile1 = file("/a/source")
      val sourceNode1: SourceNode = SourceFile(sourceFile1)
      val props1 = BuildStamp("someprops1")
      val props1_1 = BuildStamp("someprops1_1")

      val sourceFile2 = file("/b/source")
      val sourceNode2: SourceNode = SourceFile(sourceFile2)
      val props2 = BuildStamp("someprops2")

      val sourceFile3 = file("/c/source")
      val sourceNode3: SourceNode = SourceFile(sourceFile3)
      val props3 = BuildStamp("someprops3")

      val (fg, modifiedSources) = ModifiedFiles(
        new AgedSourceFileGraph(
          Graph(
            (sourceNode1 ~+> props1_1)("xyz"),
            (sourceNode2 ~+> props2)("xyz"),
            (sourceNode2 ~+> props2)("abc"),
            (sourceNode3 ~+> props3)("abc")
          ),
          Graph(
            (sourceNode1 ~+> props1)("xyz"),
            (sourceNode2 ~+> props2)("xyz")
          )
        )
      )

      fg.g.order must_== 6
      fg.g.graphSize must_== 3
      fg.g.get(sourceNode1).diSuccessors must_== Set(props1)
      fg.g.get(sourceNode2).diSuccessors must_== Set(props2)
      fg.g.get(sourceNode3).diSuccessors must_== Set(props3)
      modifiedSources.size must_== 1
      modifiedSources(0) must_== sourceFile1
    }

    "modify a source file that is depended on by another and determine only the parent" in {
      val sourceFile1 = file("/a/source")
      val sourceNode1: SourceNode = SourceFile(sourceFile1)
      val props1 = BuildStamp("someprops1")

      val sourceFile2 = file("/b/source")
      val sourceNode2: SourceNode = SourceFile(sourceFile2)
      val props2 = BuildStamp("someprops2")
      val props2_1 = BuildStamp("someprops2_1")

      val (fg, modifiedSources) = ModifiedFiles(
        new AgedSourceFileGraph(
          Graph(
            (sourceNode1 ~+> props1)("props"),
            (sourceNode2 ~+> props2)("props"),
            (sourceNode1 ~+> sourceNode2)("inputs")
          ),
          Graph(
            (sourceNode1 ~+> props1)("props"),
            (sourceNode2 ~+> props2_1)("props")
          )
        )
      )

      fg.g.order must_== 4
      fg.g.graphSize must_== 3

      val modifiedPredecessors = fg.predecessors(modifiedSources)

      modifiedPredecessors.size must_== 1
      modifiedPredecessors.head must_== sourceFile1
    }
  }
}
