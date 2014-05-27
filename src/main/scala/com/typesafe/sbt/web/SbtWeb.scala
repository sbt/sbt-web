package com.typesafe.sbt.web

import sbt._
import sbt.Keys._
import akka.actor.{ActorSystem, ActorRefFactory}
import scala.tools.nsc.util.ScalaClassLoader.URLClassLoader
import org.webjars.{WebJarExtractor, FileSystemCache}
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.incremental.{OpResult, OpSuccess}
import sbt.File

object Import {

  val Assets = config("web-assets")
  val TestAssets = config("web-assets-test")
  val Plugin = config("web-plugin")

  val pipelineStages = SettingKey[Seq[TaskKey[Pipeline.Stage]]]("web-pipeline-stages", "Sequence of tasks for the asset pipeline stages.")

  object WebKeys {

    val public = SettingKey[File]("web-public", "The location of files intended for publishing to the web.")
    val webTarget = SettingKey[File]("assets-target", "The target directory for assets")

    val jsFilter = SettingKey[FileFilter]("web-js-filter", "The file extension of js files.")
    val reporter = TaskKey[LoggerReporter]("web-reporter", "The reporter to use for conveying processing results.")

    val nodeModuleDirectory = SettingKey[File]("web-node-module-directory", "Default node modules directory, used for node based resources.")
    val nodeModuleDirectories = SettingKey[Seq[File]]("web-node-module-directories", "The list of directories that node modules are to expand into.")
    val nodeModuleGenerators = SettingKey[Seq[Task[Seq[File]]]]("web-node-module-generators", "List of tasks that generate node modules.")
    val nodeModules = TaskKey[Seq[File]]("web-node-modules", "All node module files.")

    val webModuleDirectory = SettingKey[File]("web-module-directory", "Default web modules directory, used for web browser based resources.")
    val webModuleDirectories = SettingKey[Seq[File]]("web-module-directories", "The list of directories that web browser modules are to expand into.")
    val webModuleGenerators = SettingKey[Seq[Task[Seq[File]]]]("web-module-generators", "List of tasks that generate web browser modules.")
    val webModulesLib = SettingKey[String]("web-modules-lib", "The sub folder of the path to extract web browser modules to")
    val webModules = TaskKey[Seq[File]]("web-modules", "All web browser module files.")

    val webJarsNodeModulesDirectory = SettingKey[File]("web-jars-node-modules-directory", "The path to extract WebJar node modules to")
    val webJarsNodeModules = TaskKey[Seq[File]]("web-jars-node-modules", "Produce the WebJar based node modules.")

    val webJarsDirectory = SettingKey[File]("web-jars-directory", "The path to extract WebJars to")
    val webJarsCache = SettingKey[File]("web-jars-cache", "The path for the webjars extraction cache file")
    val webJarsClassLoader = TaskKey[ClassLoader]("web-jars-classloader", "The classloader to extract WebJars from")
    val webJars = TaskKey[Seq[File]]("web-jars", "Produce the WebJars")

    val deduplicators = TaskKey[Seq[Deduplicator]]("web-deduplicators", "Functions that select from duplicate asset mappings")

    val assets = TaskKey[File]("assets", "All of the web assets.")

    val allPipelineStages = TaskKey[Pipeline.Stage]("web-all-pipeline-stages", "All asset pipeline stages chained together.")
    val pipeline = TaskKey[Seq[PathMapping]]("web-pipeline", "Run all stages of the asset pipeline.")

    val stage = TaskKey[File]("web-stage", "Create a local directory with all the files laid out as they would be in the final distribution.")
    val stagingDirectory = SettingKey[File]("web-staging-directory", "Directory where we stage distributions/releases.")

  }

  object WebJs {
    type JS[A] = js.JS[A]
    val JS = js.JS

    type JavaScript = js.JavaScript
    val JavaScript = js.JavaScript
  }

}

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
 *   --+ web
 *   ----+ public
 *   ------+ main
 *   --------+ css
 *   --------+ images
 *   --------+ js
 *   ------+ test
 *   --------+ css
 *   --------+ images
 *   --------+ js
 *   ----+ stage
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
 * "public in Assets".
 *
 * All assets be them subject to processing or static in nature, will be copied to the public destinations.
 *
 * How files are organised within "assets" or "public" is subject to the taste of the developer, their team and
 * conventions at large.
 *
 * The "stage" directory is product of processing the asset pipeline and results in files prepared for deployment
 * to a web server.
 */

object SbtWeb extends AutoPlugin {

  val autoImport = Import

  import autoImport._
  import WebKeys._

  override def globalSettings: Seq[Setting[_]] = super.globalSettings ++ Seq(
    onLoad in Global := (onLoad in Global).value andThen load,
    onUnload in Global := (onUnload in Global).value andThen unload
  )

  override def projectSettings: Seq[Setting[_]] = Seq(
    reporter := new LoggerReporter(5, streams.value.log),

    webTarget := target.value / "web",

    sourceDirectory in Assets := (sourceDirectory in Compile).value / "assets",
    sourceDirectory in TestAssets := (sourceDirectory in Test).value / "assets",
    sourceManaged in Assets := webTarget.value / "assets-managed" / "main",
    sourceManaged in TestAssets := webTarget.value / "assets-managed" / "test",

    jsFilter in Assets := GlobFilter("*.js"),
    jsFilter in TestAssets := GlobFilter("*Test.js") | GlobFilter("*Spec.js"),

    resourceDirectory in Assets := (sourceDirectory in Compile).value / "public",
    resourceDirectory in TestAssets := (sourceDirectory in Test).value / "public",
    resourceManaged in Assets := webTarget.value / "resources-managed" / "main",
    resourceManaged in TestAssets := webTarget.value / "resources-managed" / "test",

    public in Assets := webTarget.value / "public" / "main",
    public in TestAssets := webTarget.value / "public" / "test",

    nodeModuleDirectory in Assets := webTarget.value / "node-modules" / "main",
    nodeModuleDirectory in TestAssets := webTarget.value / "node-modules" / "test",
    nodeModuleDirectory in Plugin := (target in Plugin).value / "node-modules",

    webModuleDirectory in Assets := webTarget.value / "web-modules" / "main",
    webModuleDirectory in TestAssets := webTarget.value / "web-modules" / "test",
    webModulesLib := "lib",

    webJarsCache in webJars in Assets := webTarget.value / "web-modules" / "webjars-main.cache",
    webJarsCache in webJars in TestAssets := webTarget.value / "web-modules" / "webjars-test.cache",
    webJarsCache in nodeModules in Assets := webTarget.value / "node-modules" / "webjars-main.cache",
    webJarsCache in nodeModules in TestAssets := webTarget.value / "node-modules" / "webjars-test.cache",
    webJarsCache in nodeModules in Plugin := (target in Plugin).value / "webjars-plugin.cache",
    webJarsClassLoader in Assets := new URLClassLoader((dependencyClasspath in Compile).value.map(_.data.toURI.toURL), null),
    webJarsClassLoader in TestAssets := {
      // Exclude webjars from compile, because they are already extracted to assets
      val isCompileDep: Set[File] = (dependencyClasspath in Compile).value.map(_.data).toSet
      val testClasspath: Classpath = (dependencyClasspath in Test).value
      val urls = testClasspath.collect {
        case dep if !isCompileDep(dep.data) => dep.data.toURI.toURL
      }

      new URLClassLoader(urls, null)
    },
    webJarsClassLoader in Plugin := SbtWeb.getClass.getClassLoader,

    assets in Assets := syncMappings(
      streams.value.cacheDirectory,
      (mappings in Assets).value,
      (public in Assets).value
    ),
    assets in TestAssets := syncMappings(
      streams.value.cacheDirectory,
      (mappings in Assets).value ++ (mappings in TestAssets).value,
      (public in TestAssets).value
    ),
    assets := (assets in Assets).value,

    compile in Assets := inc.Analysis.Empty,
    compile in TestAssets := inc.Analysis.Empty,
    compile in TestAssets <<= (compile in TestAssets).dependsOn(compile in Assets),

    test in TestAssets :=(),
    test in TestAssets <<= (test in TestAssets).dependsOn(compile in TestAssets),

    watchSources <++= unmanagedSources in Assets,
    watchSources <++= unmanagedSources in TestAssets,
    watchSources <++= unmanagedResources in Assets,
    watchSources <++= unmanagedResources in TestAssets,

    pipelineStages := Seq.empty,
    allPipelineStages <<= Pipeline.chain(pipelineStages),
    pipeline := allPipelineStages.value((mappings in Assets).value),

    deduplicators := Nil,
    pipeline := deduplicateMappings(pipeline.value, deduplicators.value),

    stagingDirectory := webTarget.value / "stage",
    stage := syncMappings(
      streams.value.cacheDirectory,
      pipeline.value,
      stagingDirectory.value
    ),

    baseDirectory in Plugin := (baseDirectory in LocalRootProject).value / "project",
    target in Plugin := (baseDirectory in Plugin).value / "target",
    crossTarget in Plugin := Defaults.makeCrossTarget((target in Plugin).value, scalaBinaryVersion.value, sbtBinaryVersion.value, plugin = true, crossPaths.value)

  ) ++
    inConfig(Assets)(unscopedAssetSettings) ++ inConfig(Assets)(nodeModulesSettings) ++
    inConfig(TestAssets)(unscopedAssetSettings) ++ inConfig(TestAssets)(nodeModulesSettings) ++
    inConfig(Plugin)(nodeModulesSettings)


  val unscopedAssetSettings: Seq[Setting[_]] = Seq(
    includeFilter := GlobFilter("*"),

    sourceGenerators := Nil,
    managedSourceDirectories := Nil,
    managedSources := sourceGenerators(_.join).map(_.flatten).value,
    unmanagedSourceDirectories := Seq(sourceDirectory.value),
    unmanagedSources := (unmanagedSourceDirectories.value ** (includeFilter.value -- excludeFilter.value)).get,
    sourceDirectories := managedSourceDirectories.value ++ unmanagedSourceDirectories.value,
    sources := managedSources.value ++ unmanagedSources.value,

    resourceGenerators := Nil,
    managedResourceDirectories := Nil,
    managedResources := resourceGenerators(_.join).map(_.flatten).value,
    unmanagedResourceDirectories := Seq(resourceDirectory.value),
    unmanagedResources := (unmanagedResourceDirectories.value ** (includeFilter.value -- excludeFilter.value)).get,
    resourceDirectories := managedResourceDirectories.value ++ unmanagedResourceDirectories.value,
    resources := managedResources.value ++ unmanagedResources.value,

    webModuleGenerators := Nil,
    webModuleGenerators <+= webJars,
    webModuleDirectories := Seq(webJarsDirectory.value),
    webModules := webModuleGenerators(_.join).map(_.flatten).value,

    webJarsDirectory := webModuleDirectory.value / "webjars",
    webJars := generateWebJars(webJarsDirectory.value, webModulesLib.value, (webJarsCache in webJars).value, webJarsClassLoader.value),

    mappings := {
      val files = (sources.value ++ resources.value ++ webModules.value) ---
        (sourceDirectories.value ++ resourceDirectories.value ++ webModuleDirectories.value)
      files pair relativeTo(sourceDirectories.value ++ resourceDirectories.value ++ webModuleDirectories.value) | flat
    },

    pipelineStages := Seq.empty,
    allPipelineStages <<= Pipeline.chain(pipelineStages),
    mappings := allPipelineStages.value(mappings.value),

    deduplicators := Nil,
    mappings := deduplicateMappings(mappings.value, deduplicators.value)
  )

  val nodeModulesSettings = Seq(
    webJarsNodeModulesDirectory := nodeModuleDirectory.value / "webjars",
    webJarsNodeModules := generateNodeWebJars(webJarsNodeModulesDirectory.value, (webJarsCache in nodeModules).value, webJarsClassLoader.value),

    nodeModuleGenerators := Nil,
    nodeModuleGenerators <+= webJarsNodeModules,
    nodeModuleDirectories := Seq(webJarsNodeModulesDirectory.value),
    nodeModules := nodeModuleGenerators(_.join).map(_.flatten).value
  )


  private def withWebJarExtractor(to: File, cacheFile: File, classLoader: ClassLoader)
                                 (block: (WebJarExtractor, File) => Unit): File = {
    val cache = new FileSystemCache(cacheFile)
    val extractor = new WebJarExtractor(cache, classLoader)
    block(extractor, to)
    cache.save()
    to
  }

  private def generateNodeWebJars(target: File, cache: File, classLoader: ClassLoader): Seq[File] = {
    withWebJarExtractor(target, cache, classLoader) {
      (e, to) =>
        e.extractAllNodeModulesTo(to)
    }.***.get
  }

  private def generateWebJars(target: File, lib: String, cache: File, classLoader: ClassLoader): Seq[File] = {
    withWebJarExtractor(target / lib, cache, classLoader) {
      (e, to) =>
        e.extractAllWebJarsTo(to)
    }
    target.***.get
  }

  // Mapping deduplication

  /**
   * Deduplicate mappings with a sequence of deduplicator functions.
   *
   * A mapping has duplicates if multiple files map to the same relative path.
   * The deduplicators are applied to the duplicate files and the first
   * successful deduplication is used (when a deduplicator selects a file),
   * otherwise the duplicate mappings are left and an error will be reported
   * when syncing the mappings.
   *
   * @param mappings the mappings to deduplicate
   * @param deduplicators the deduplicating functions
   * @return the (possibly) deduplicated mappings
   */
  def deduplicateMappings(mappings: Seq[PathMapping], deduplicators: Seq[Deduplicator]): Seq[PathMapping] = {
    if (deduplicators.isEmpty) {
      mappings
    } else {
      mappings.groupBy(_._2 /*path*/).toSeq flatMap { grouped =>
        val (path, group) = grouped
        if (group.size > 1) {
          val files = group.map(_._1)
          val deduplicated = firstResult(deduplicators)(files)
          deduplicated.fold(group)(file => Seq((file, path)))
        } else {
          group
        }
      }
    }
  }

  /**
   * Return the result of the first Some returning function.
   */
  private def firstResult[A, B](fs: Seq[A => Option[B]])(a: A): Option[B] = {
    (fs.toStream flatMap { f => f(a).toSeq }).headOption
  }

  /**
   * Deduplicator that selects the first file contained in the base directory.
   *
   * @param base the base directory to check against
   * @return a deduplicator function that prefers files in the base directory
   */
  def selectFileFrom(base: File): Deduplicator = {
    (files: Seq[File]) => files.find(_.relativeTo(base).isDefined)
  }

  // Mapping synchronization

  /**
   * Efficiently synchronize a sequence of mappings with a target folder.
   * @param cacheDir the cache directory.
   * @param mappings the mappings to sync.
   * @param target  the destination directory to sync to.
   * @return the target value
   */
  def syncMappings(cacheDir: File, mappings: Seq[PathMapping], target: File): File = {
    val cache = cacheDir / "sync-mappings"
    val copies = mappings map {
      case (file, path) => file -> (target / path)
    }
    Sync(cache)(copies)
    target
  }

  // Resource extract API

  /**
   * Copy a resource to a target folder.
   *
   * The resource won't be copied if the new file is older.
   *
   * @param to the target folder.
   * @param url the url of the resource.
   * @param cacheDir the dir to cache whether the file was read or not.
   * @return the copied file.
   */
  def copyResourceTo(to: File, url: URL, cacheDir: File): File = {
    incremental.runIncremental(cacheDir, Seq(url)) {
      ops =>
        val fromFile = if (url.getProtocol == "file") {
          new File(url.toURI)
        } else if (url.getProtocol == "jar") {
          new File(url.getFile.split('!')(0))
        } else {
          throw new RuntimeException(s"Unknown protocol: $url")
        }

        val toFile = to / fromFile.getName

        if (ops.nonEmpty) {
          val is = url.openStream()
          try {
            toFile.getParentFile.mkdirs()
            IO.transfer(is, toFile)
            (Map(url -> OpSuccess(Set(fromFile), Set(toFile))), toFile)
          } finally is.close()
        } else {
          (Map.empty[URL, OpResult], toFile)
        }
    }
  }

  // Actor system management and API

  private val webActorSystemAttrKey = AttributeKey[ActorSystem]("web-actor-system")

  private def load(state: State): State = {
    state.get(webActorSystemAttrKey).fold({
      val webActorSystem = withActorClassloader(ActorSystem("sbt-web"))
      state.put(webActorSystemAttrKey, webActorSystem)
    })(as => state)
  }

  private def unload(state: State): State = {
    state.get(webActorSystemAttrKey).fold(state) {
      as =>
        as.shutdown()
        state.remove(webActorSystemAttrKey)
    }
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
