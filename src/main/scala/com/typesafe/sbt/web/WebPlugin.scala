package com.typesafe.sbt.web

import sbt._
import sbt.Keys._
import akka.actor.{ActorSystem, ActorRefFactory}
import akka.util.Timeout
import scala.concurrent.duration._

/**
 * Adds settings concerning themselves with web things to SBT. Here is the directory structure supported by this plugin
 * showing relevant sbt settings:
 *
 * {{{
 *   + src
 *   --+ main
 *   ----+ assets .....(sourceDirectory in Assets)
 *   ------+ js
 *   ----+ public .....(resourceDirectory in Assets)
 *   ------+ css
 *   ------+ images
 *   ------+ js
 *   --+ test
 *   ----+ assets .....(sourceDirectory in TestAssets)
 *   ------+ js
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
 *
 * All assets be them subject to processing or static in nature, will be copied to the resourceManaged destinations.
 *
 * How files are organised within "assets" or "public" is subject to the taste of the developer, their team and
 * conventions at large.
 */

object WebPlugin extends sbt.Plugin {

  object WebKeys {
    val Assets = config("web-assets")
    val TestAssets = config("web-assets-test")
    val jsDirName = SettingKey[String]("web-js-dir", "The name of JavaScript directories.")
    val jsFilter = SettingKey[FileFilter]("web-js-filter", "The file extension of js files.")
    val reporter = TaskKey[LoggerReporter]("web-reporter", "The reporter to use for conveying processing results.")
  }

  private def copyFiles(sources: Seq[File], target: File): Seq[(File, File)] = {
    val copyDescs: Seq[(File, File)] = (for {
      source: File <- sources
    } yield {
      (source ** "*") filter (!_.isDirectory) x Path.rebase(source, target)
    }).flatten
    IO.copy(copyDescs)
    copyDescs
  }

  private def locateSources(sourceDirectories: Seq[File], includeFilter: FileFilter, excludeFilter: FileFilter): Seq[File] =
    (sourceDirectories ** (includeFilter -- excludeFilter)).get

  import WebKeys._

  override def globalSettings: Seq[Setting[_]] = super.globalSettings ++ Seq(
    reporter := new LoggerReporter(5, streams.value.log),

    sourceDirectory in Assets := (sourceDirectory in Compile).value / "assets",
    sourceDirectory in TestAssets := (sourceDirectory in Test).value / "assets",

    unmanagedSourceDirectories in Assets := Seq((sourceDirectory in Assets).value),
    unmanagedSourceDirectories in TestAssets := Seq((sourceDirectory in TestAssets).value),
    jsFilter in Assets := GlobFilter("*.js"),
    jsFilter in TestAssets  := GlobFilter("*Test.js") | GlobFilter("*Spec.js"),
    includeFilter in Assets := (jsFilter in Assets).value,
    includeFilter in TestAssets := (jsFilter in TestAssets).value,
    unmanagedSources in Assets <<= (unmanagedSourceDirectories in Assets, includeFilter in Assets, excludeFilter in Assets) map locateSources,
    unmanagedSources in TestAssets <<= (unmanagedSourceDirectories in TestAssets, includeFilter in TestAssets, excludeFilter in TestAssets) map locateSources,

    resourceDirectory in Assets := (sourceDirectory in Compile).value / "public",
    resourceDirectory in TestAssets := (sourceDirectory in Test).value / "public",
    resourceManaged in Assets := target.value / "public",
    resourceManaged in TestAssets := target.value / "public-test",

    resourceDirectories in Assets := Seq((sourceDirectory in Assets).value, (resourceDirectory in Assets).value),
    resourceDirectories in TestAssets := Seq((sourceDirectory in TestAssets).value, (resourceDirectory in TestAssets).value),

    copyResources in Assets <<= (resourceDirectories in Assets, resourceManaged in Assets) map copyFiles,
    copyResources in Compile <<= (copyResources in Compile).dependsOn(copyResources in Assets),
    copyResources in TestAssets <<= (resourceDirectories in TestAssets, resourceManaged in TestAssets) map copyFiles,
    copyResources in Test <<= (copyResources in Test).dependsOn(copyResources in TestAssets),

    onLoad in Global := (onLoad in Global).value andThen (load),
    onUnload in Global := (onUnload in Global).value andThen (unload)

  )

  // Actor system management and API

  implicit val webActorTimeout = Timeout(5.seconds)

  private val webActorSystemAttrKey = AttributeKey[ActorSystem]("web-actor-system")

  private def load(state: State): State = {
    state.get(webActorSystemAttrKey).map(as => state).getOrElse {
      val webActorSystem = withActorClassloader(ActorSystem("sbt-web"))
      state.put(webActorSystemAttrKey, webActorSystem)
    }
  }

  private def unload(state: State): State = {
    state.get(webActorSystemAttrKey).map {
      as =>
        as.shutdown
        state.remove(webActorSystemAttrKey)
    }.getOrElse(state)
  }

  /**
   * Perform actor related activity with sbt-web's actor system.
   * @param state The project build state available to the task invoking this.
   * @param namespace A means by which actors can be namespaced.
   * @param block The block of code to execute.
   * @tparam T The expected return type of the block.
   * @return The return value of the block.
   */
  def withActorRefFactory[T](state: State, namespace: String)(block: (ActorRefFactory) => T): T = {
    // We will get an exception if there is no known actor system - which is a good thing because
    // there absolutely has to be at this point.
    block(state.get(webActorSystemAttrKey).get)
  }

  private def withActorClassloader[A](f: => A): A = {
    val newLoader = ActorSystem.getClass.getClassLoader
    val thread = Thread.currentThread
    val oldLoader = thread.getContextClassLoader

    thread.setContextClassLoader(newLoader)
    try {
      f
    } finally {
      thread.setContextClassLoader(oldLoader)
    }
  }
}
