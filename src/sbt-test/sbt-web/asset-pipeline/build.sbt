import com.typesafe.web.sbt.pipeline.Pipeline

webSettings

val coffee = taskKey[Unit]("mock coffeescript processing")

coffee := {
  // translate .coffee files into .js files
  val sourceDir = (sourceDirectory in WebKeys.Assets).value
  val targetDir = (resourceManaged in WebKeys.Assets).value
  val sources = sourceDir ** "*.coffee"
  val mappings = sources pair relativeTo(sourceDir)
  val renamed = mappings map { case (file, path) => file -> path.replaceAll("coffee", "js") }
  val copies = renamed map { case (file, path) => file -> (targetDir / path) }
  IO.copy(copies)
}

compile in Compile <<= (compile in Compile).dependsOn(coffee)

val jsmin = taskKey[Pipeline.Stage]("mock js minifier")

jsmin := { (mappings: Pipeline.Mappings) =>
  // pretend to combine all .js files into one .min.js file
  val targetDir = target.value / "jsmin" / "public"
  val (js, other) = mappings partition (_._2.endsWith(".js"))
  val minFile = targetDir / "js" / "all.min.js"
  IO.touch(minFile)
  val minMappings = Seq(minFile) pair relativeTo(targetDir)
  minMappings ++ other
}

WebKeys.stages <+= jsmin

val check = taskKey[Unit]("check the pipeline mappings")

check := {
  val mappings = WebKeys.pipeline.value
  val paths = (mappings map (_._2)).toSet
  val expected = Set("js/all.min.js", "coffee/a.coffee")
  if (paths != expected) sys.error(s"Expected $expected but pipeline paths are $paths")
}
