import com.typesafe.sbt.web.pipeline.Pipeline

val root = (project in file(".")).enablePlugins(SbtWeb)

libraryDependencies ++= Seq(
  "org.webjars" % "jquery" % "2.0.3-1"
)

val myPipelineTask = taskKey[Pipeline.Stage]("Some pipeline task")

myPipelineTask := identity

pipelineStages := Seq(myPipelineTask)

