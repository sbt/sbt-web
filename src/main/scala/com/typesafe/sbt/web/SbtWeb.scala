package com.typesafe.sbt.web

import sbt.{ Def, given, * }
import sbt.internal.inc.Analysis
import sbt.internal.io.Source
import sbt.Keys.*
import sbt.Defaults.relativeMappings
import scala.language.implicitConversions
import org.webjars.WebJarExtractor
import org.webjars.WebJarAssetLocator.WEBJARS_PATH_PREFIX
import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.incremental.{ OpResult, OpSuccess, toStringInputHasher }
import xsbti.{ Reporter, FileConverter }

import com.typesafe.sbt.PluginCompat.*

object Import {

  val Assets = config("web-assets")
  val TestAssets = config("web-assets-test").extend(Assets)
  val Plugin = config("web-plugin")

  val pipelineStages =
    SettingKey[Seq[TaskKey[Pipeline.Stage]]]("web-pipeline-stages", "Sequence of tasks for the asset pipeline stages.")

  object WebKeys {

    val public = SettingKey[File]("web-public", "The location of files intended for publishing to the web.")
    val webTarget = SettingKey[File]("assets-target", "The target directory for assets")

    val jsFilter = SettingKey[FileFilter]("web-js-filter", "The file extension of js files.")
    val reporter = TaskKey[Reporter]("web-reporter", "The reporter to use for conveying processing results.")

    val nodeModuleDirectory =
      SettingKey[File]("web-node-module-directory", "Default node modules directory, used for node based resources.")
    val nodeModuleDirectories = SettingKey[Seq[File]](
      "web-node-module-directories",
      "The list of directories that node modules are to expand into."
    )
    val nodeModuleGenerators =
      SettingKey[Seq[Task[Seq[File]]]]("web-node-module-generators", "List of tasks that generate node modules.")
    val nodeModules = TaskKey[Seq[File]]("web-node-modules", "All node module files.")

    val webModuleDirectory =
      SettingKey[File]("web-module-directory", "Default web modules directory, used for web browser based resources.")
    val webModuleDirectories = SettingKey[Seq[File]](
      "web-module-directories",
      "The list of directories that web browser modules are to expand into."
    )
    val webModuleGenerators =
      SettingKey[Seq[Task[Seq[File]]]]("web-module-generators", "List of tasks that generate web browser modules.")
    val webModulesLib =
      SettingKey[String]("web-modules-lib", "The sub folder of the path to extract web browser modules to")
    val webModules = TaskKey[Seq[File]]("web-modules", "All web browser module files.")

    val internalWebModules =
      TaskKey[Seq[String]]("web-internal-web-modules", "Web modules that are on the internal dependency classpath.")
    val importDirectly = SettingKey[Boolean](
      "web-import-directly",
      "Determine whether internal web modules should be imported directly by default."
    )
    val directWebModules =
      TaskKey[Seq[String]]("web-direct-web-modules", "Web modules that should be used without 'lib/module' prefix.")

    val webJarsNodeModulesDirectory =
      SettingKey[File]("web-jars-node-modules-directory", "The path to extract WebJar node modules to")
    val webJarsNodeModules = TaskKey[Seq[File]]("web-jars-node-modules", "Produce the WebJar based node modules.")

    val webJarsDirectory = SettingKey[File]("web-jars-directory", "The path to extract WebJars to")
    val webJarsCache = SettingKey[File]("web-jars-cache", "The path for the webjars extraction cache file")
    val webJarsClassLoader = TaskKey[ClassLoader]("web-jars-classloader", "The classloader to extract WebJars from")
    val webJars = TaskKey[Seq[File]]("web-jars", "Produce the WebJars")

    val deduplicators =
      TaskKey[Seq[Deduplicator]]("web-deduplicators", "Functions that select from duplicate asset mappings")

    val assets = TaskKey[File]("assets", "All of the web assets.")

    val exportedMappings =
      TaskKey[Seq[PathMapping]]("web-exported-mappings", "Asset mappings in WebJar format for exporting and packaging.")
    val addExportedMappingsToPackageBinMappings =
      SettingKey[Boolean](
        "web-add-exportedmappings-to-packagebin-mappings",
        "If exportedMappings should be added to the PackageBin mappings"
      )
    val exportedAssets = TaskKey[File]("web-exported-directory", "Directory with assets in WebJar format.")
    val exportedAssetsIfMissing = TaskKey[File](
      "web-exported-directory-if-missing",
      "Directory with assets in WebJar format, but only when missing from a tracking perspective."
    )
    val exportedAssetsNoTracking = TaskKey[File](
      "web-exported-directory-no-tracking",
      "Directory with assets in WebJar format, but no tracking from a tracking perspective."
    )

    val allPipelineStages =
      TaskKey[Pipeline.Stage]("web-all-pipeline-stages", "All asset pipeline stages chained together.")
    val pipeline = TaskKey[Seq[PathMapping]]("web-pipeline", "Run all stages of the asset pipeline.")

    val packagePrefix = SettingKey[String]("web-package-prefix", "Path prefix when packaging all assets.")

    val stage = TaskKey[File](
      "web-stage",
      "Create a local directory with all the files laid out as they would be in the final distribution."
    )
    val stagingDirectory = SettingKey[File]("web-staging-directory", "Directory where we stage distributions/releases.")

    val disableExportedProducts = AttributeKey[Boolean](
      "disableExportedProducts",
      "If added to the state and set to true, assets will not be exported via exportedProducts"
    )
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
 *   ----+ assets .....(Assets / sourceDirectory)
 *   ------+ js
 *   ----+ public .....(Assets / resourceDirectory)
 *   ------+ css
 *   ------+ images
 *   ------+ js
 *   --+ test
 *   ----+ assets .....(TestAssets / sourceDirectory)
 *   ------+ js
 *   ----+ public .....(TestAssets / resourceDirectory)
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
 * consumption e.g. by a browser. This is also distinct from sbt's existing notion of "resources" as project resources
 * are generally not made public by a web server. The name "assets" heralds from Rails.
 *
 * "public" denotes a type of asset that does not require processing i.e. these resources are static in nature.
 *
 * In sbt, asset source files are considered the source for plugins that process them. When they are processed any
 * resultant files become public. For example a coffeescript plugin would use files from "unmanagedSources in Assets"
 * and produce them to "public in Assets".
 *
 * All assets be them subject to processing or static in nature, will be copied to the public destinations.
 *
 * How files are organised within "assets" or "public" is subject to the taste of the developer, their team and
 * conventions at large.
 *
 * The "stage" directory is product of processing the asset pipeline and results in files prepared for deployment to a
 * web server.
 */

object SbtWeb extends AutoPlugin {

  val autoImport = Import

  override def requires = sbt.plugins.JvmPlugin

  import autoImport.*
  import WebKeys.*

  override def projectConfigurations: Seq[Configuration] =
    super.projectConfigurations ++ Seq(Assets, TestAssets, Plugin)

  override def buildSettings: Seq[Def.Setting[?]] = Seq(
    (Plugin / nodeModuleDirectory) := (Plugin / target).value / "node-modules",
    (Plugin / nodeModules / webJarsCache) := (Plugin / target).value / "webjars-plugin.cache",
    (Plugin / webJarsClassLoader) := SbtWeb.getClass.getClassLoader,
    (Plugin / baseDirectory) := (LocalRootProject / baseDirectory).value / "project",
    (Plugin / target) := (Plugin / baseDirectory).value / "target",
    (Plugin / crossTarget) := Defaults.makeCrossTarget(
      (Plugin / target).value,
      scalaVersion.value,
      scalaBinaryVersion.value,
      sbtBinaryVersion.value,
      plugin = true,
      crossPaths.value
    )
  ) ++ inConfig(Plugin)(nodeModulesSettings)

  override def projectSettings: Seq[Setting[?]] = Seq(
    reporter := new CompileProblems.LoggerReporter(5, streams.value.log),
    webTarget := target.value / "web",
    (Assets / sourceDirectory) := (Compile / sourceDirectory).value / "assets",
    (TestAssets / sourceDirectory) := (Test / sourceDirectory).value / "assets",
    (Assets / sourceManaged) := webTarget.value / "assets-managed" / "main",
    (TestAssets / sourceManaged) := webTarget.value / "assets-managed" / "test",
    (Assets / jsFilter) := GlobFilter("*.js"),
    (TestAssets / jsFilter) := GlobFilter("*Test.js") | GlobFilter("*Spec.js"),
    (Assets / resourceDirectory) := (Compile / sourceDirectory).value / "public",
    (TestAssets / resourceDirectory) := (Test / sourceDirectory).value / "public",
    (Assets / resourceManaged) := webTarget.value / "resources-managed" / "main",
    (TestAssets / resourceManaged) := webTarget.value / "resources-managed" / "test",
    (Assets / public) := webTarget.value / "public" / "main",
    (TestAssets / public) := webTarget.value / "public" / "test",
    (Assets / classDirectory) := webTarget.value / "classes" / "main",
    (TestAssets / classDirectory) := webTarget.value / "classes" / "test",
    (Assets / nodeModuleDirectory) := webTarget.value / "node-modules" / "main",
    (TestAssets / nodeModuleDirectory) := webTarget.value / "node-modules" / "test",
    (Assets / webModuleDirectory) := webTarget.value / "web-modules" / "main",
    (TestAssets / webModuleDirectory) := webTarget.value / "web-modules" / "test",
    webModulesLib := "lib",
    (Assets / internalWebModules) := getInternalWebModules(Compile).value,
    (TestAssets / internalWebModules) := getInternalWebModules(Test).value,
    importDirectly := false,
    (Assets / directWebModules) := Nil,
    (TestAssets / directWebModules) := Seq((Assets / moduleName).value),
    (Assets / webJars / webJarsCache) := webTarget.value / "web-modules" / "webjars-main.cache",
    (TestAssets / webJars / webJarsCache) := webTarget.value / "web-modules" / "webjars-test.cache",
    (Assets / nodeModules / webJarsCache) := webTarget.value / "node-modules" / "webjars-main.cache",
    (TestAssets / nodeModules / webJarsCache) := webTarget.value / "node-modules" / "webjars-test.cache",
    (Assets / webJarsClassLoader) := classLoader((Compile / dependencyClasspath).value, fileConverter.value),
    (TestAssets / webJarsClassLoader) := classLoader((Test / dependencyClasspath).value, fileConverter.value),
    assets := (Assets / assets).value,
    (Compile / packageBin / mappings) ++= {
      if ((Assets / addExportedMappingsToPackageBinMappings).value) {
        (Assets / exportedMappings).value
      } else {
        Nil
      }
    },
    (Test / packageBin / mappings) ++= {
      if ((TestAssets / addExportedMappingsToPackageBinMappings).value) {
        (TestAssets / exportedMappings).value
      } else {
        Nil
      }
    },
    (Compile / exportedProducts) ++= exportAssets(Assets, Compile, TrackLevel.TrackAlways).value,
    (Test / exportedProducts) ++= exportAssets(TestAssets, Test, TrackLevel.TrackAlways).value,
    (Compile / exportedProductsIfMissing) ++= exportAssets(Assets, Compile, TrackLevel.TrackIfMissing).value,
    (Test / exportedProductsIfMissing) ++= exportAssets(TestAssets, Test, TrackLevel.TrackIfMissing).value,
    (Compile / exportedProductsNoTracking) ++= exportAssets(Assets, Compile, TrackLevel.NoTracking).value,
    (Test / exportedProductsNoTracking) ++= exportAssets(TestAssets, Test, TrackLevel.NoTracking).value,
    (Assets / compile) := Analysis.Empty,
    (TestAssets / compile) := Analysis.Empty,
    (TestAssets / compile) := (TestAssets / compile).dependsOn(Assets / compile).value,
    (TestAssets / test) := (()),
    (TestAssets / test) := (TestAssets / test).dependsOn(TestAssets / compile).value,
    addWatchSources(unmanagedSources, unmanagedSourceDirectories, Assets),
    addWatchSources(unmanagedSources, unmanagedSourceDirectories, TestAssets),
    addWatchSources(unmanagedResources, unmanagedResourceDirectories, Assets),
    addWatchSources(unmanagedResources, unmanagedResourceDirectories, TestAssets),
    pipelineStages := Seq.empty,
    allPipelineStages := Pipeline.chain(pipelineStages).value,
    pipeline := allPipelineStages.value((Assets / mappings).value),
    deduplicators := Nil,
    pipeline := deduplicateMappings(pipeline.value, deduplicators.value, fileConverter.value),
    stagingDirectory := webTarget.value / "stage",
    stage := syncMappings(
      streams.value.cacheStoreFactory.make("sync-stage"),
      pipeline.value,
      stagingDirectory.value,
      fileConverter.value
    )
  ) ++
    inConfig(Assets)(unscopedAssetSettings) ++ inConfig(Assets)(nodeModulesSettings) ++
    inConfig(TestAssets)(unscopedAssetSettings) ++ inConfig(TestAssets)(nodeModulesSettings) ++
    packageSettings

  val unscopedAssetSettings: Seq[Setting[?]] = Seq(
    includeFilter := GlobFilter("*"),
    sourceGenerators := Nil,
    managedSourceDirectories := Nil,
    managedSources := sourceGenerators(_.join).map(_.flatten).value,
    unmanagedSourceDirectories := Seq(sourceDirectory.value),
    unmanagedSources := unmanagedSourceDirectories.value
      .descendantsExcept(includeFilter.value, excludeFilter.value)
      .get(),
    sourceDirectories := managedSourceDirectories.value ++ unmanagedSourceDirectories.value,
    sources := managedSources.value ++ unmanagedSources.value,
    (sources / mappings) := relativeMappings(sources, sourceDirectories).value,
    resourceGenerators := Nil,
    managedResourceDirectories := Nil,
    managedResources := resourceGenerators(_.join).map(_.flatten).value,
    unmanagedResourceDirectories := Seq(resourceDirectory.value),
    unmanagedResources := unmanagedResourceDirectories.value
      .descendantsExcept(includeFilter.value, excludeFilter.value)
      .get(),
    resourceDirectories := managedResourceDirectories.value ++ unmanagedResourceDirectories.value,
    resources := managedResources.value ++ unmanagedResources.value,
    (resources / mappings) := relativeMappings(resources, resourceDirectories).value,
    webModuleGenerators := Nil,
    webModuleDirectories := Nil,
    webModules := webModuleGenerators(_.join).map(_.flatten).value,
    (webModules / mappings) := relativeMappings(webModules, webModuleDirectories).value,
    (webModules / mappings) := flattenDirectWebModules.value,
    directWebModules ++= {
      val modules = internalWebModules.value
      if (importDirectly.value) modules else Seq.empty
    },
    webJarsDirectory := webModuleDirectory.value / "webjars",
    webJars := generateWebJars(
      webJarsDirectory.value,
      webModulesLib.value,
      (webJars / webJarsCache).value,
      webJarsClassLoader.value
    ),
    webModuleGenerators += webJars.taskValue,
    webModuleDirectories += webJarsDirectory.value,
    mappings := (sources / mappings).value ++ (resources / mappings).value ++ (webModules / mappings).value,
    pipelineStages := Seq.empty,
    allPipelineStages := Pipeline.chain(pipelineStages).value,
    mappings := allPipelineStages.value(mappings.value),
    deduplicators := Nil,
    mappings := deduplicateMappings(mappings.value, deduplicators.value, fileConverter.value),
    assets := syncMappings(
      streams.value.cacheStoreFactory.make(s"sync-assets-" + configuration.value.name),
      mappings.value,
      public.value,
      fileConverter.value
    ),
    exportedMappings := createWebJarMappings.value,
    addExportedMappingsToPackageBinMappings := true,
    exportedAssets := syncExportedAssets(TrackLevel.TrackAlways).value,
    exportedAssetsIfMissing := syncExportedAssets(TrackLevel.TrackIfMissing).value,
    exportedAssetsNoTracking := syncExportedAssets(TrackLevel.NoTracking).value,
    exportedProducts := exportProducts(exportedAssets).value,
    exportedProductsIfMissing := exportProducts(exportedAssetsIfMissing).value,
    exportedProductsNoTracking := exportProducts(exportedAssetsNoTracking).value
  )

  val nodeModulesSettings = Seq(
    webJarsNodeModulesDirectory := nodeModuleDirectory.value / "webjars",
    webJarsNodeModules := generateNodeWebJars(
      webJarsNodeModulesDirectory.value,
      (nodeModules / webJarsCache).value,
      webJarsClassLoader.value
    ),
    nodeModuleGenerators := Nil,
    nodeModuleGenerators += webJarsNodeModules.taskValue,
    nodeModuleDirectories := Seq(webJarsNodeModulesDirectory.value),
    nodeModules := nodeModuleGenerators(_.join).map(_.flatten).value
  )

  private def addWatchSources(
      unmanagedSourcesKey: TaskKey[Seq[File]],
      unmanagedSourceDirectoriesKey: SettingKey[Seq[File]],
      scopeKey: Configuration
  ) = {
    Keys.watchSources ++= {
      val include = (scopeKey / unmanagedSourcesKey / Keys.includeFilter).value
      val exclude = (scopeKey / unmanagedSourcesKey / Keys.excludeFilter).value

      (scopeKey / unmanagedSourceDirectoriesKey).value.map { directory =>
        new Source(directory, include, exclude)
      }
    }
  }

  def webJarsPathPrefix: Def.Initialize[Task[String]] = Def.task {
    path(s"$WEBJARS_PATH_PREFIX/${moduleName.value}/${version.value}/")
  }

  def syncExportedAssets(track: TrackLevel): Def.Initialize[Task[File]] = Def.taskDyn {
    val syncTargetDir = classDirectory.value
    val syncRequired = TrackLevel.intersection(track, exportToInternal.value) match {
      case TrackLevel.TrackAlways =>
        true
      case TrackLevel.TrackIfMissing | TrackLevel.NoTracking =>
        !(syncTargetDir / webJarsPathPrefix.value).exists()
    }
    if (syncRequired) Def.task {
      state.value.log.debug(s"Exporting ${configuration.value}:${moduleName.value}")
      syncMappings(
        streams.value.cacheStoreFactory.make("sync-exported-assets-" + configuration.value.name),
        exportedMappings.value,
        syncTargetDir,
        fileConverter.value
      )
    }
    else
      Def.task(syncTargetDir)
  }

  /**
   * Create package mappings for assets in the webjar format. Use the webjars path prefix and exclude all web module
   * assets.
   */
  def createWebJarMappings: Def.Initialize[Task[Seq[(FileRef, String)]]] = Def.task {
    def webModule(file: File) = webModuleDirectories.value.exists(dir => IO.relativize(dir, file).isDefined)
    implicit val fc: FileConverter = fileConverter.value
    mappings.value flatMap {
      case (file, path) if webModule(toFile(file)) => None
      case (file, path)                            => Some(file -> (webJarsPathPrefix.value + path))
    }
  }

  def exportAssets(
      assetConf: Configuration,
      exportConf: Configuration,
      track: TrackLevel
  ): Def.Initialize[Task[Classpath]] = Def.taskDyn {
    if ((exportConf / exportJars).value) Def.task {
      Seq.empty
    }
    else {
      track match {
        case TrackLevel.TrackAlways =>
          assetConf / exportedProducts
        case TrackLevel.TrackIfMissing =>
          assetConf / exportedProductsIfMissing
        case TrackLevel.NoTracking =>
          assetConf / exportedProductsNoTracking
      }
    }
  }

  def exportProducts(exportTask: TaskKey[File]): Def.Initialize[Task[Classpath]] = Def.task {
    if (state.value.get(disableExportedProducts).getOrElse(false)) {
      Seq.empty
    } else {
      implicit val fc: FileConverter = fileConverter.value
      Seq(Attributed.blank(toFileRef(exportTask.value)).put(toKey(webModulesLib), moduleName.value))
    }
  }

  def packageSettings: Seq[Setting[_]] = inConfig(Assets)(
    Defaults.packageTaskSettings(packageBin, packageAssetsMappings) ++ Seq(
      packagePrefix := "",
      Keys.`package` := packageBin.value
    )
  )

  /**
   * Create package mappings for all assets, adding the optional prefix.
   */
  def packageAssetsMappings: Def.Initialize[Task[Seq[(FileRef, String)]]] = Def.task {
    val prefix = packagePrefix.value
    (Defaults.ConfigGlobal / pipeline).value map { case (file, path) =>
      file -> (prefix + path)
    }
  }

  /**
   * Get module names for all internal web module dependencies on the classpath.
   */
  def getInternalWebModules(conf: Configuration): Def.Initialize[Task[Seq[String]]] = Def.task {
    (conf / internalDependencyClasspath).value.flatMap(_.get(toKey(WebKeys.webModulesLib)))
  }

  /**
   * Remove web module dependencies from a classpath. This is a helper method for Play 2.3 transitions.
   */
  def classpathWithoutAssets(classpath: Classpath): Classpath = {
    classpath.filter(_.get(toKey(WebKeys.webModulesLib)).isEmpty)
  }

  def flattenDirectWebModules = Def.task {
    val directModules = directWebModules.value
    val moduleMappings = (webModules / mappings).value
    val lib = webModulesLib.value
    if (directModules.nonEmpty) {
      val prefixes = directModules map { module =>
        path(s"$lib/$module/")
      }
      moduleMappings map { case (file, path) =>
        file -> stripPrefixes(path, prefixes)
      }
    } else {
      moduleMappings
    }
  }

  /**
   * Transform normalized paths into platform-specific paths.
   */
  def path(normalized: String, separator: Char = java.io.File.separatorChar): String = {
    if (separator == '/') normalized else normalized.replace('/', separator)
  }

  private def stripPrefixes(s: String, prefixes: Seq[String]): String = {
    prefixes.find(s.startsWith).fold(s)(s.stripPrefix)
  }

  private def classLoader(classpath: Classpath, conv: FileConverter): ClassLoader = {
    implicit val fc: FileConverter = conv
    new java.net.URLClassLoader(Path.toURLs(classpathToFiles(classpath)), null)
  }

  private def withWebJarExtractor(to: File, cacheFile: File, classLoader: ClassLoader)(
      block: (WebJarExtractor, File) => Unit
  ): File = {
    val extractor = new WebJarExtractor(classLoader)
    block(extractor, to)
    to
  }

  private def generateNodeWebJars(target: File, cache: File, classLoader: ClassLoader): Seq[File] = {
    withWebJarExtractor(target, cache, classLoader) { (e, to) =>
      e.extractAllNodeModulesTo(to)
    }.**(AllPassFilter).get()
  }

  private def generateWebJars(target: File, lib: String, cache: File, classLoader: ClassLoader): Seq[File] = {
    withWebJarExtractor(target / lib, cache, classLoader) { (e, to) =>
      e.extractAllWebJarsTo(to)
    }
    target.**(AllPassFilter).get()
  }

  // Mapping deduplication

  /**
   * Deduplicate mappings with a sequence of deduplicator functions.
   *
   * A mapping has duplicates if multiple files map to the same relative path. The deduplicators are applied to the
   * duplicate files and the first successful deduplication is used (when a deduplicator selects a file), otherwise the
   * duplicate mappings are left and an error will be reported when syncing the mappings.
   *
   * @param mappings
   *   the mappings to deduplicate
   * @param deduplicators
   *   the deduplicating functions
   * @return
   *   the (possibly) deduplicated mappings
   */
  def deduplicateMappings(
      mappings: Seq[PathMapping],
      deduplicators: Seq[Deduplicator],
      conv: FileConverter
  ): Seq[PathMapping] = {
    implicit val fc: FileConverter = conv
    if (deduplicators.isEmpty) {
      mappings
    } else {
      mappings.groupBy(_._2 /*path*/ ).toSeq flatMap { grouped =>
        val (path, group) = grouped
        if (group.size > 1) {
          val files = group.map(mapping => toFile(mapping._1))
          val deduplicated = firstResult(deduplicators)(files)
          deduplicated.fold(group)(file => Seq((toFileRef(file), path)))
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
   * Convert a file to a FileRef, for compatibility usage in user sbt files/tasks
   * @param file
   *   The file to convert
   * @param conv
   *   A valid FileConverter. Usually fileConverter.value, in Task scope
   * @return
   *   The file converted to the cross-buildable FileRef type.
   */
  def asFileRef(file: File, conv: FileConverter): FileRef = {
    implicit val fc: FileConverter = conv
    toFileRef(file)
  }

  /**
   * Deduplicator that selects the first file contained in the base directory.
   *
   * @param base
   *   the base directory to check against
   * @return
   *   a deduplicator function that prefers files in the base directory
   */
  def selectFileFrom(base: File): Deduplicator = { (files: Seq[File]) =>
    files.find(_.relativeTo(base).isDefined)
  }

  /**
   * Deduplicator that checks whether all duplicates are directories and if so will simply select the first one.
   *
   * @return
   *   a deduplicator function for duplicate directories
   */
  def selectFirstDirectory: Deduplicator = selectFirstWhen { files =>
    files.forall(_.isDirectory)
  }

  /**
   * Deduplicator that checks that all duplicates have the same contents hash and if so will simply select the first
   * one.
   *
   * @return
   *   a deduplicator function for checking identical contents
   */
  def selectFirstIdenticalFile: Deduplicator = selectFirstWhen { files =>
    files.forall(_.isFile) && files.map(_.hashString).distinct.size == 1
  }

  private def selectFirstWhen(predicate: Seq[File] => Boolean): Deduplicator = { (files: Seq[File]) =>
    if (predicate(files)) files.headOption else None
  }

  // Mapping synchronization

  /**
   * Efficiently synchronize a sequence of mappings with a target folder.
   *
   * @param cacheStore
   *   the cache store.
   * @param mappings
   *   the mappings to sync.
   * @param target
   *   the destination directory to sync to.
   * @return
   *   the target value
   */
  def syncMappings(
      cacheStore: sbt.util.CacheStore,
      mappings: Seq[PathMapping],
      target: File,
      conv: FileConverter
  ): File = {
    implicit val fc: FileConverter = conv
    val copies = mappings map { case (file, path) =>
      toFile(file) -> (target / path)
    }
    Sync.sync(cacheStore)(copies)
    target
  }

  // Resource extract API

  /**
   * Copy a resource to a target folder.
   *
   * The resource won't be copied if the new file is older.
   *
   * @param to
   *   the target folder.
   * @param url
   *   the url of the resource.
   * @param cacheDir
   *   the dir to cache whether the file was read or not.
   * @return
   *   the copied file.
   */
  def copyResourceTo(to: File, url: URL, cacheDir: File): File = {
    incremental
      .syncIncremental(cacheDir, Seq(url)) { ops =>
        val fromFile = if (url.getProtocol == "file") {
          new File(url.toURI)
        } else if (url.getProtocol == "jar") {
          new File(url.getFile.split('!').last)
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
      ._2
  }
}
