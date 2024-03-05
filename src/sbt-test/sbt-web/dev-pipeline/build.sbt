import com.typesafe.sbt.web.pipeline.Pipeline
import com.typesafe.sbt.web.PathMapping
import WebKeys._

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

val transform = taskKey[Pipeline.Stage]("js transformer")

transform := {
  val targetDir = target.value / "transform"

  { (mappings: Seq[PathMapping]) =>
    // transform js files - rename as .new.js just for testing
    val (jsMappings, otherMappings) = mappings partition (_._2.endsWith(".js"))
    val transformedMappings = jsMappings map { case (file, path) =>
      val newPath = path.dropRight(3) + ".new.js"
      val newFile = targetDir / newPath
      IO.touch(newFile)
      newFile -> newPath
    }
    transformedMappings ++ otherMappings
  }
}

pipelineStages in Assets := Seq(transform)
