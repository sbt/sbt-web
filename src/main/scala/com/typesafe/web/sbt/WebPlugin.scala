package com.typesafe.web.sbt

import sbt._
import sbt.Keys._
import akka.actor.{ActorSystem, ActorRefFactory}
import akka.util.Timeout
import scala.concurrent.duration._
import scala.tools.nsc.util.ScalaClassLoader.URLClassLoader
import org.webjars.{WebJarExtractor, FileSystemCache}

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

    import sbt.KeyRanks._

    val Assets = config("web-assets")
    val TestAssets = config("web-assets-test")
    val jsDirName = SettingKey[String]("web-js-dir", "The name of JavaScript directories.")
    val jsFilter = SettingKey[FileFilter]("web-js-filter", "The file extension of js files.")
    val reporter = TaskKey[LoggerReporter]("web-reporter", "The reporter to use for conveying processing results.")

    val extractWebJars = TaskKey[File]("web-extract-web-jars", "Extract webjars", ATask)
    val extractWebJarsPath = SettingKey[File]("web-extract-web-jars-path", "The path to extract WebJars to", ASetting)
    val extractWebJarsCache = SettingKey[File]("web-extract-web-jars-cache", "The path for the webjars extraction cache file", CSetting)
    val allWebJars = TaskKey[Seq[String]]("web-all-web-jars", "Discover all WebJars on the classpath", CTask)
    val includeWebJars = SettingKey[Seq[String]]("web-include-web-jars", "Include WebJars with the given name", BSetting)
    val excludeWebJars = SettingKey[Seq[String]]("web-exclude-web-jars", "Exclude WebJars with the given name", BSetting)
    val extractWebJarsClassLoader = TaskKey[ClassLoader]("web-extract-web-jars-classloader", "The classloader to extract WebJars from", CTask)
    val nodeModules = TaskKey[File]("web-node-modules", "The locations of node modules for all related webjars.")
  }

  import WebKeys._

  override def globalSettings: Seq[Setting[_]] = super.globalSettings ++ Seq(
    onLoad in Global := (onLoad in Global).value andThen (load),
    onUnload in Global := (onUnload in Global).value andThen (unload)
  )

  def webSettings: Seq[Setting[_]] = Seq(
    reporter := new LoggerReporter(5, streams.value.log),

    sourceDirectory in Assets := (sourceDirectory in Compile).value / "assets",
    sourceDirectory in TestAssets := (sourceDirectory in Test).value / "assets",

    jsFilter in Assets := GlobFilter("*.js"),
    jsFilter in TestAssets := GlobFilter("*Test.js") | GlobFilter("*Spec.js"),
    includeFilter in TestAssets := (jsFilter in TestAssets).value,

    resourceDirectory in Assets := (sourceDirectory in Compile).value / "public",
    resourceDirectory in TestAssets := (sourceDirectory in Test).value / "public",
    resourceManaged in Assets := target.value / "public",
    resourceManaged in TestAssets := target.value / "public-test",

    copyResources in Compile <<= (copyResources in Compile).dependsOn(copyResources in Assets),
    copyResources in Test <<= (copyResources in Test).dependsOn(copyResources in TestAssets),

    extractWebJarsPath in Assets := target.value / "webjars",
    extractWebJarsPath in TestAssets := target.value / "webjars-test",
    extractWebJarsCache in Assets := target.value / "webjars.cache",
    extractWebJarsCache in TestAssets := target.value / "webjars-test.cache",
    includeWebJars in Assets := Seq("*"),
    // By default, don't extract any web jars in test
    includeWebJars in TestAssets := Nil,
    extractWebJarsClassLoader in Assets <<= (dependencyClasspath in Compile).map {
      modules =>
        new URLClassLoader(modules.map(_.data.toURI.toURL), null)
    },
    extractWebJarsClassLoader in TestAssets <<= (dependencyClasspath in Test).map {
      modules =>
        new URLClassLoader(modules.map(_.data.toURI.toURL), null)
    }

  ) ++ inConfig(Assets)(scopedSettings) ++ inConfig(TestAssets)(scopedSettings)

  private val extractWebJarsSettings: Seq[Setting[_]] = Seq(
    excludeWebJars := Nil,
    allWebJars <<= extractWebJarsClassLoader.map(ExtractWebJars.discoverWebJars),
    extractWebJars <<= (allWebJars, extractWebJarsClassLoader, includeWebJars, excludeWebJars, extractWebJarsPath,
      extractWebJarsCache) map {
      (webJars, classLoader, includes, excludes, path, cache) =>
        ExtractWebJars.extractWebJars(classLoader, ExtractWebJars.filterWebJars(webJars, includes, excludes), path, cache)
    },
    nodeModules <<= (extractWebJarsClassLoader, extractWebJarsPath, extractWebJarsCache) map {
      (classLoader, path, cache) =>
        copyNodeModulesTo(path / WebJarExtractor.NODE_MODULES, cache, classLoader)
    }

  )

  private val scopedSettings: Seq[Setting[_]] = Seq(
    unmanagedSourceDirectories := Seq(sourceDirectory.value),
    includeFilter := jsFilter.value,
    unmanagedSources <<= (unmanagedSourceDirectories, includeFilter, excludeFilter) map locateSources,
    resourceDirectories := Seq(sourceDirectory.value, resourceDirectory.value),
    copyResources <<= (resourceDirectories, resourceManaged) map copyFiles
  ) ++ extractWebJarsSettings

  private def locateSources(sourceDirectories: Seq[File], includeFilter: FileFilter, excludeFilter: FileFilter): Seq[File] =
    (sourceDirectories ** (includeFilter -- excludeFilter)).get

  private def copyFiles(sources: Seq[File], target: File): Seq[(File, File)] = {
    val copyDescs: Seq[(File, File)] = (for {
      source: File <- sources
    } yield {
      (source ** "*") filter (!_.isDirectory) x Path.rebase(source, target)
    }).flatten
    IO.copy(copyDescs)
    copyDescs
  }

  // Actor system management and API

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

  /**
   * Copy a resource to a target folder.
   * @param to the target folder.
   * @param name the name of the resource.
   * @param classLoader the class loader to use.
   * @return the copied file.
   */
  def copyResourceTo(to: File, name: String, classLoader: ClassLoader): File = {
    val f = to / name
    if (!f.exists()) {
      val is = classLoader.getResourceAsStream(name)
      try {
        IO.transfer(is, f)
        f
      } finally {
        is.close()
      }
    } else {
      f
    }
  }

  /**
   * Copy node modules from all WebJars.
   * @param to the target folder
   * @param cacheFile the file to be used for the cache.
   * @param classLoader  the class loader to use.
   * @return the target folder used.
   */
  def copyNodeModulesTo(to: File, cacheFile: File, classLoader: ClassLoader): File = {
    val cache = new FileSystemCache(cacheFile)
    val extractor = new WebJarExtractor(cache, classLoader)
    extractor.extractAllNodeModulesTo(to)
    cache.save()
    to
  }
}
