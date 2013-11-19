package com.typesafe.js.sbt

import sbt._
import sbt.Keys._

/**
 * Adds settings concerning themselves with web things to SBT. Here is the directory structure described by this plugin:
 *
 * {{{
 *   + src
 *   --+ main
 *   ----+ assets .....(sourceDirectory in Assets)
 *   ------+ js .......(jsSource in Assets)
 *   ----+ public .....(resourceDirectory in Assets)
 *   ------+ css
 *   ------+ images
 *   ------+ js
 *   --+ test
 *   ----+ assets .....(sourceDirectory in TestAssets)
 *   ------+ js .......(jsSource in TestAssets)
 *   ----+ public .....(resourceDirectory in TestAssets)
 *   ------+ css
 *   ------+ images
 *   ------+ js
 *
 *   + target
 *   --+ public .......(resourceManaged in Assets)
 *   ----+ css
 *   ----+ images
 *   ----+ js
 *   --+ public-test ..(resourceManaged in TestAssets)
 *   ----+ css
 *   ----+ images
 *   ----+ js
 * }}}
 *
 * The plugin introduces the notion of "assets" to sbt. Assets are public resources that are intended for client-side
 * consumption e.g. by a browser. This is also distinct from sbt's existing notion of "resources" as
 * project resources are generally not made public by a web server. The name "assets" heralds from Rails.
 *
 * "public" denotes a type of asset that does not require processing i.e. these resources are static in nature.
 *
 * In sbt, asset source files are considered the source for plugins that process them. When they are processed any resultant
 * files become public. For example a coffeescript plugin would use files from "unmanagedSources in Assets" and produce them to
 * a "jsDirName" inside "resourceManaged in Assets".
 */
object WebPlugin extends sbt.Plugin {

  object WebKeys {
    val Assets = config("web-assets")
    val TestAssets = config("web-assets-test")
    val jsDirName = SettingKey[String]("web-js-dir", "The name of JavaScript directories.")
    val jsSource = SettingKey[File]("web-js-source", "The main source directory for JavaScript.")
    val jsFilter = SettingKey[FileFilter]("web-js-filter", "The file extension of regular js files.")
    val jsTestFilter = SettingKey[FileFilter]("web-js-test-filter", "The file extension of test js files.")
    val reporter = TaskKey[LoggerReporter]("web-reporter", "The reporter to use for conveying processing results.")
  }

  private def copyFiles(source: File, target: File): Seq[(File, File)] = {
    import scala.language.postfixOps
    val sources = (PathFinder(source) ***) x Path.rebase(source, target)
    IO.copy(sources)
    sources
  }

  private def locateSources(sourceDirectories: Seq[File], includeFilter: FileFilter, excludeFilter: FileFilter): Seq[File] =
    (sourceDirectories ** (includeFilter -- excludeFilter)).get

  import WebKeys._

  override def globalSettings: Seq[Setting[_]] = super.globalSettings ++ Seq(
    reporter := new LoggerReporter(5, streams.value.log)
  )

  override def projectSettings: Seq[Setting[_]] = super.projectSettings ++ Seq(
    sourceDirectory in Assets := (sourceDirectory in Compile).value / "assets",
    sourceDirectory in TestAssets := (sourceDirectory in Test).value / "assets",

    jsDirName := "js",
    jsSource in Assets := (sourceDirectory in Assets).value / jsDirName.value,
    jsSource in TestAssets := (sourceDirectory in Test).value / jsDirName.value,
    unmanagedSourceDirectories in Assets := Seq((jsSource in Assets).value),
    unmanagedSourceDirectories in TestAssets := Seq((jsSource in TestAssets).value),
    jsFilter := GlobFilter("*.js"),
    jsTestFilter := GlobFilter("*Test.js") | GlobFilter("*Spec.js"),
    includeFilter in Assets := jsFilter.value,
    includeFilter in TestAssets := jsTestFilter.value,
    unmanagedSources in Assets <<= (unmanagedSourceDirectories in Assets, includeFilter in Assets, excludeFilter in Assets) map locateSources,
    unmanagedSources in TestAssets <<= (unmanagedSourceDirectories in TestAssets, includeFilter in TestAssets, excludeFilter in TestAssets) map locateSources,

    resourceDirectory in Assets := (sourceDirectory in Compile).value / "public",
    resourceDirectory in TestAssets := (sourceDirectory in Test).value / "public",
    resourceManaged in Assets := target.value / "public",
    resourceManaged in TestAssets := target.value / "public-test",
    copyResources in Assets <<= (resourceDirectory in Assets, resourceManaged in Assets) map copyFiles,
    copyResources in Compile <<= (copyResources in Compile).dependsOn(copyResources in Assets),
    copyResources in TestAssets <<= (resourceDirectory in TestAssets, resourceManaged in TestAssets) map copyFiles,
    copyResources in Test <<= (copyResources in Test).dependsOn(copyResources in TestAssets)
  )
}
