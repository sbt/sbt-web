package com.typesafe.js.sbt

import sbt._
import sbt.Keys._

/**
 * Adds settings concerning themselves with web things to SBT. Here is the directory structure that we need to
 * support:
 * {{{
 *   + src
 *   --+ assets
 *   ----+ js
 *   ----+ css
 *   --+ test
 *   ----+ js
 *   ----+ css
 *
 *   + target
 *   --+ public
 *   ----+ js
 *   ----+ css
 *   --+ public-test
 *   ----+ js
 *   ----+ css
 * }}}
 *
 * The plugin introduces the notion of "assets" to sbt. Assets are public resources that are indended for client-side
 * consumption i.e. to be consumed by a browser. This is distinct from sbt's existing notion of "resources" as
 * project resources are generally not made public by a web server. The name "assets" heralds from Rails.
 */
object WebPlugin extends sbt.Plugin {

  object WebKeys {
    val Assets = config("web-assets")
    val AssetsTest = config("web-assets-test")
    val jsSource = SettingKey[File]("web-js-source", "The main source directory for JavaScript.")
    val reporter = TaskKey[LoggerReporter]("web-reporter", "The reporter to use for conveying processing results.")
  }

  private def locateSources(sourceDirectory: File, includeFilter: FileFilter, excludeFilter: FileFilter): Seq[File] =
    (sourceDirectory ** (includeFilter -- excludeFilter)).get

  import WebKeys._

  override def globalSettings: Seq[Setting[_]] = super.globalSettings ++ Seq(
    reporter := new LoggerReporter(5, streams.value.log)
  )

  override def projectSettings: Seq[Setting[_]] = super.projectSettings ++ Seq(
    sourceDirectory in Assets := baseDirectory.value / "src" / "assets",
    sourceDirectory in AssetsTest := (sourceDirectory in Test).value,

    jsSource in Assets := (sourceDirectory in Assets).value / "js",
    jsSource in AssetsTest := (sourceDirectory in Test).value / "js",

    includeFilter in Assets := GlobFilter("*.js"),
    includeFilter in AssetsTest := GlobFilter("*Test.js") | GlobFilter("*Spec.js"),
    unmanagedSources in Assets <<= (jsSource in Assets, includeFilter in Assets, excludeFilter in Assets) map locateSources,
    unmanagedSources in AssetsTest <<= (jsSource in AssetsTest, includeFilter in AssetsTest, excludeFilter in AssetsTest) map locateSources,

    resourceManaged in Assets := target.value / "public",
    resourceManaged in AssetsTest := target.value / "public-test"
  )
}
