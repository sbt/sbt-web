import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.SbtWebPlugin.WebKeys._
import com.typesafe.sbt.web.PathMapping

webSettings

val coffee = taskKey[Seq[PathMapping]]("mock coffeescript processing")

coffee := {
  // translate .coffee files into .js files
  val sourceDir = (sourceDirectory in WebKeys.Assets).value
  val targetDir = target.value / "cs-plugin"
  val sources = sourceDir ** "*.coffee"
  val mappings = sources pair relativeTo(sourceDir)
  val renamed = mappings map { case (file, path) => file -> path.replaceAll("coffee", "js") }
  val copies = renamed map { case (file, path) => file -> (targetDir / path) }
  IO.copy(copies)
  mappings ++ (copies map { case (s, t) => t} pair relativeTo(targetDir))
}

assetTasks in Assets <+= coffee

val jsmin = taskKey[Pipeline.Stage]("mock js minifier")

jsmin := { (mappings: Seq[PathMapping]) =>
  // pretend to combine all .js files into one .min.js file
  val targetDir = target.value / "jsmin" / "public"
  val (js, other) = mappings partition (_._2.endsWith(".js"))
  val minFile = targetDir / "js" / "all.min.js"
  IO.touch(minFile)
  val minMappings = Seq(minFile) pair relativeTo(targetDir)
  minMappings ++ other
}

stages <+= jsmin

val check = taskKey[Unit]("check the pipeline mappings")

check := {
  val mappings = pipeline.value
  val paths = (mappings map (_._2)).toSet
  val expected = Set("js/all.min.js", "coffee/a.coffee")
  if (paths != expected) sys.error(s"Expected $expected but pipeline paths are $paths")
}
