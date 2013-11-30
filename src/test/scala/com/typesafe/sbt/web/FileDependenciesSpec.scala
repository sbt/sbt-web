package com.typesafe.sbt.web

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import java.io.File
import sbt.GlobFilter
import scalax.collection.Graph
import scalax.collection.GraphEdge.DiEdge
import scalax.collection.GraphPredef._

@RunWith(classOf[JUnitRunner])
class FileDependenciesSpec extends Specification {

  def createTempJsFile(prefix: String): File = {
    val f = File.createTempFile(prefix, ".js")
    f.deleteOnExit()
    f
  }

  "file dependencies" should {
    "return all new files as modified given an empty graph" in {
      val unmanagedSource = createTempJsFile("source")
      val unmanagedSources = Seq(unmanagedSource)
      val jsFile = GlobFilter("*.js")
      val targetResource = createTempJsFile("target")
      val copiedResources = Seq((unmanagedSource, targetResource))
      val lastJsSourceGraph = Graph[File, DiEdge]()

      val (newJsSourceGraph, modifiedJsSources) = ModifiedFiles(
        new PendingFileGraph(lastJsSourceGraph, unmanagedSources, jsFile, copiedResources))

      newJsSourceGraph.g.graphSize must_== 1
      newJsSourceGraph.g.get(unmanagedSource).diSuccessors must_== Set(targetResource)
      modifiedJsSources.size must_== 1
    }

    "return all existing files as unmodified given a previous graph" in {
      val unmanagedSource = createTempJsFile("source")
      val unmanagedSources = Seq(unmanagedSource)
      val jsFile = GlobFilter("*.js")
      val targetResource = createTempJsFile("target")
      val copiedResources = Seq((unmanagedSource, targetResource))
      val lastJsSourceGraph = Graph(unmanagedSource ~> targetResource)

      val (newJsSourceGraph, modifiedJsSources) = ModifiedFiles(
        new PendingFileGraph(lastJsSourceGraph, unmanagedSources, jsFile, copiedResources))

      newJsSourceGraph.g.graphSize must_== 1
      newJsSourceGraph.g.get(unmanagedSource).diSuccessors must_== Set(targetResource)
      modifiedJsSources.size must_== 0
    }

    "return one existing file as modified given a previous graph with a removed file" in {
      val unmanagedSource = createTempJsFile("source")
      val unmanagedSources = Seq(unmanagedSource)
      val jsFile = GlobFilter("*.js")
      val targetResource = createTempJsFile("target")
      val copiedResources = Seq((unmanagedSource, targetResource))
      val deletedUnmanagedSource = createTempJsFile("source")
      val deletedTargetResource = createTempJsFile("target")
      val lastJsSourceGraph = Graph(deletedUnmanagedSource ~> deletedTargetResource)

      val (newJsSourceGraph, modifiedJsSources) = ModifiedFiles(
        new PendingFileGraph(lastJsSourceGraph, unmanagedSources, jsFile, copiedResources))

      newJsSourceGraph.g.graphSize must_== 1
      newJsSourceGraph.g.get(unmanagedSource).diSuccessors must_== Set(targetResource)
      modifiedJsSources.size must_== 1
    }

    "return one existing file as modified given a previous graph" in {
      val unmanagedSource1 = createTempJsFile("source")
      val unmanagedSource2 = createTempJsFile("source")
      val unmanagedSources = Seq(unmanagedSource1, unmanagedSource2)
      val jsFile = GlobFilter("*.js")
      val targetResource1 = createTempJsFile("target")
      targetResource1.setLastModified(unmanagedSource1.lastModified() - 1)
      val targetResource2 = createTempJsFile("target")
      targetResource2.setLastModified(unmanagedSource2.lastModified() + 1)
      val copiedResources = Seq((unmanagedSource1, targetResource1), (unmanagedSource2, targetResource2))
      val lastJsSourceGraph = Graph(unmanagedSource1 ~> targetResource1, unmanagedSource2 ~> targetResource2)

      val (newJsSourceGraph, modifiedJsSources) = ModifiedFiles(
        new PendingFileGraph(lastJsSourceGraph, unmanagedSources, jsFile, copiedResources))

      newJsSourceGraph.g.graphSize must_== 2
      newJsSourceGraph.g.get(unmanagedSource1).diSuccessors must_== Set(targetResource1)
      newJsSourceGraph.g.get(unmanagedSource2).diSuccessors must_== Set(targetResource2)
      modifiedJsSources.size must_== 1
      modifiedJsSources(0) must_== unmanagedSource1
    }
  }
}
