import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.PathMapping
import WebKeys._

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

val coffee = taskKey[Seq[File]]("mock coffeescript processing")

coffee := {
  // translate .coffee files into .js files
  val sourceDir = (Assets / sourceDirectory).value
  val targetDir = target.value / "cs-plugin"
  val sources = sourceDir ** "*.coffee"
  val mappings = sources pair Path.relativeTo(sourceDir)
  val renamed = mappings map { case (file, path) => file -> path.replaceAll("coffee", "js") }
  val copies = renamed map { case (file, path) => file -> (Assets / resourceManaged).value / path }
  IO.copy(copies)
  copies map (_._2)
}

Assets / sourceGenerators += coffee.taskValue

val jsmin = taskKey[Pipeline.Stage]("mock js minifier")

jsmin := {
  val targetDir = target.value / "jsmin" / "public"

  { (mappings: Seq[PathMapping]) =>
    // pretend to combine all .js files into one .min.js file
    val (js, other) = mappings partition (_._2.endsWith(".js"))
    val minFile = targetDir / "js" / "all.min.js"
    IO.touch(minFile)
    val minMappings = Seq(minFile) pair Path.relativeTo(targetDir)
    val convertedMinMappings = minMappings.map{ case (file, path) =>
      SbtWeb.asFileRef(file, fileConverter.value) -> path
    }
    convertedMinMappings ++ other
  }
}

pipelineStages := Seq(jsmin)

TaskKey[Unit]("fileCheck") := {
  assert((target.value / "jsmin" / "public" / "js" / "all.min.js").exists())
  assert((target.value / "web" / "stage" / "coffee" / "a.coffee").exists())
}