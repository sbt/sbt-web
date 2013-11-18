package com.typesafe.js.sbt

import sbt._
import sbt.Keys._

/**
 * Adds settings concerning themselves with web things to SBT
 */
object WebPlugin extends sbt.Plugin {

  object WebKeys {
    val Web = config("web")
    val WebTest = config("web-test")
    val jsSource = SettingKey[File]("web-js-source", "The main source directory for JavaScript.")
    val reporter = TaskKey[LoggerReporter]("web-reporter", "The reporter to use for conveying processing results.")
  }

  private def filterSources(sources: Seq[File], includeFilter: FileFilter, excludeFilter: FileFilter): Seq[File] = {
    val filter = includeFilter -- excludeFilter
    sources.filter(filter.accept)
  }

  private def locateSources(sourceDirectory: File, includeFilter: FileFilter, excludeFilter: FileFilter): Seq[File] =
    (sourceDirectory ** (includeFilter -- excludeFilter)).get

  import WebKeys._

  override def globalSettings: Seq[Setting[_]] = super.globalSettings ++ Seq(
    reporter := new LoggerReporter(5, streams.value.log)
  )

  override def projectSettings: Seq[Setting[_]] = super.projectSettings ++ Seq(
    includeFilter in Web := GlobFilter("*.js"),
    includeFilter in WebTest := GlobFilter("*Test.js") | GlobFilter("*Spec.js"),

    // jsSource is just a directory that allows you to layout your project nicely, anything in it gets added to the
    // resources task.
    jsSource in Compile := (sourceDirectory in Compile).value / "js",
    jsSource in Test := (sourceDirectory in Test).value / "js",
    unmanagedResources in Compile <++= (jsSource in Compile, includeFilter in(Compile, unmanagedResources), excludeFilter in(Compile, unmanagedResources)) map locateSources,
    unmanagedResources in Test <++= (jsSource in Test, includeFilter in(Test, unmanagedResources), excludeFilter in(Test, unmanagedResources)) map locateSources,

    // The actual javascript sources come from whatever is in resources
    unmanagedSources in Web <<= (unmanagedResources in Compile, includeFilter in Web, excludeFilter in Web) map filterSources,
    managedSources in Web <<= (managedResources in Compile, includeFilter in Web, excludeFilter in Web) map filterSources,
    unmanagedSources in WebTest <<= (unmanagedResources in Test, includeFilter in WebTest, excludeFilter in WebTest) map filterSources,
    managedSources in WebTest <<= (managedResources in Test, includeFilter in WebTest, excludeFilter in WebTest) map filterSources,
    sources in Web <<= (unmanagedSources in Web, managedSources in Web) map (_ ++ _),
    sources in WebTest <<= (unmanagedSources in WebTest, managedSources in WebTest) map (_ ++ _),

    // The class directory in the web scope is useful to describe where publically available resources should be placed e.g.
    // the result of a less compilation should copied into this target.
    classDirectory in Web := (classDirectory in Compile).value / "public"
  )
}
