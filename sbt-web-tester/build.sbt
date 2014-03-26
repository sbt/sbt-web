import com.typesafe.sbt.web.SbtWebPlugin

val root = project.in(file(".")).addPlugins(SbtWebPlugin)

libraryDependencies ++= Seq(
  "org.webjars" % "jquery" % "2.0.3-1"
)
