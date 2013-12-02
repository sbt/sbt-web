package com.typesafe.sbt.web

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import java.io.File
import scalax.collection.Graph
import scalax.collection.edge.Implicits._
import scalax.collection.edge.LDiEdge

@RunWith(classOf[JUnitRunner])
class FileDependenciesSpec extends Specification {

  def createTempFile(prefix: String): File = {
    val f = File.createTempFile(prefix, ".")
    f.deleteOnExit()
    f
  }

  "file dependencies" should {
    "return all new files as modified given an empty graph" in {
      val unmanagedSource = createTempFile("source")
      val targetResource = createTempFile("target")

      val (fg, modifiedSources) = ModifiedFiles(
        new PendingFileGraph(
          "xyz",
          Graph[File, LDiEdge](),
          Graph((unmanagedSource ~+> targetResource)("xyz"))
        )
      )

      fg.g.graphSize must_== 1
      fg.g.get(unmanagedSource).diSuccessors must_== Set(targetResource)
      modifiedSources.size must_== 1
    }

    "return all existing files as unmodified given a previous graph" in {
      val unmanagedSource = createTempFile("source")
      val targetResource = createTempFile("target")

      val (fg, modifiedSources) = ModifiedFiles(
        new PendingFileGraph(
          "xyz",
          Graph((unmanagedSource ~+> targetResource)("xyz")),
          Graph((unmanagedSource ~+> targetResource)("xyz"))
        )
      )

      fg.g.graphSize must_== 1
      fg.g.get(unmanagedSource).diSuccessors must_== Set(targetResource)
      modifiedSources.size must_== 0
    }

    "return one existing file as modified given a previous graph with a removed file" in {
      val unmanagedSource = createTempFile("source")
      val targetResource = createTempFile("target")
      val deletedUnmanagedSource = createTempFile("source")
      val deletedTargetResource = createTempFile("target")

      val (fg, modifiedSources) = ModifiedFiles(
        new PendingFileGraph(
          "xyz",
          Graph((deletedUnmanagedSource ~+> deletedTargetResource)("xyz")),
          Graph((unmanagedSource ~+> targetResource)("xyz"))
        )
      )

      fg.g.graphSize must_== 1
      fg.g.get(unmanagedSource).diSuccessors must_== Set(targetResource)
      modifiedSources.size must_== 1
    }

    "return one existing file as modified given a previous graph along with unrelated nodes given their labels" in {
      val unmanagedSource1 = createTempFile("source")
      val targetResource1 = createTempFile("target")
      val targetResource1_1 = createTempFile("target")
      val unmanagedSource2 = createTempFile("source")
      val targetResource2 = createTempFile("target")
      val unmanagedSource3 = createTempFile("source")
      val targetResource3 = createTempFile("target")

      val (fg, modifiedSources) = ModifiedFiles(
        new PendingFileGraph(
          "xyz",
          Graph(
            (unmanagedSource1 ~+> targetResource1_1)("xyz"),
            (unmanagedSource2 ~+> targetResource2)("xyz"),
            (unmanagedSource2 ~+> targetResource2)("abc"),
            (unmanagedSource3 ~+> targetResource3)("abc")
          ),
          Graph(
            (unmanagedSource1 ~+> targetResource1)("xyz"),
            (unmanagedSource2 ~+> targetResource2)("xyz")
          )
        )
      )

      fg.g.graphSize must_== 3
      fg.g.edges.size must_== 3
      fg.g.get(unmanagedSource1).diSuccessors must_== Set(targetResource1)
      fg.g.get(unmanagedSource2).diSuccessors must_== Set(targetResource2)
      modifiedSources.size must_== 1
      modifiedSources(0) must_== unmanagedSource1
    }
  }
}
