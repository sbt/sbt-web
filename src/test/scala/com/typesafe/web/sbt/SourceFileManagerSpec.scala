package com.typesafe.web.sbt

import sbt._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import scala.collection.immutable

@RunWith(classOf[JUnitRunner])
class SourceFileManagerSpec extends Specification {

  "source dependencies" should {
    "return all new files as modified given none before" in {
      val manager = SourceFileManager(file(""))
      val sources = immutable.Seq((file("somefile"), "somesetting"))
      val changedSources = manager.setAndCompareBuildStamps(sources)
      changedSources must_== sources.map(_._1)
    }

    "return all existing files as unmodified given no change in state" in {
      val manager = SourceFileManager(file(""))
      manager.setAndCompareBuildStamps(immutable.Seq(
        (file("someparent"), "somesetting"),
        (file("somechild"), "somesetting")
      ))
      val changedSources = manager.setAndCompareBuildStamps(immutable.Seq(
        (file("someparent"), "somesetting"),
        (file("somechild"), "somesetting")
      ))
      changedSources.size must_== 0
    }

    "modify a source file that is depended on by another and determine only the parent" in {
      val manager = SourceFileManager(file(""))
      val sources = immutable.Seq(
        (file("someparent"), "somesetting"),
        (file("somechild"), "somesetting")
      )
      manager.setAndCompareBuildStamps(sources)

      val inputs = immutable.Seq(
        (file("somechild"), immutable.Seq(file("someparent")))
      )
      manager.setInputs(inputs)

      manager.inputs(immutable.Seq(file("somechild"))) must_== Set(file("someparent"))
    }
  }
}
