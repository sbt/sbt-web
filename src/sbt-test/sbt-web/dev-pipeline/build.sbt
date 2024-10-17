import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.PathMapping
import WebKeys._

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

val jsTransform = taskKey[Pipeline.Stage]("js transformer")

jsTransform := {
  val targetDir = target.value / "transform"

  { (mappings: Seq[PathMapping]) =>
    // transform js files - rename as .new.js just for testing
    val (jsMappings, otherMappings) = mappings partition (_._2.endsWith(".js"))
    val transformedMappings = jsMappings map { case (file, path) =>
      val newPath = path.dropRight(3) + ".new.js"
      val newFile = targetDir / newPath
      IO.touch(newFile)
      SbtWeb.asFileRef(newFile, fileConverter.value) -> newPath
    }
    transformedMappings ++ otherMappings
  }
}

Assets / pipelineStages := Seq(jsTransform)

TaskKey[Unit]("fileCheck") := {
  assert(!( target.value / "web" / "public" / "main" / "js" / "a.js" ).exists(), "Found 'web/public/main/js/a.js', which should not exist.")
  assert(( target.value / "web" / "public" / "main" / "js" / "a.new.js" ).exists(), "Could not find 'web/public/main/js/a.new.js'")
}