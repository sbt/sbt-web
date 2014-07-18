lazy val root = (project in file(".")).enablePlugins(SbtWeb)

name := "Web Project"

version := "0.1"

crossPaths := false

libraryDependencies += "org.webjars" % "jquery" % "2.0.3-1"

TaskKey[Unit]("extractAssets") := IO.unzip((artifactPath in (Assets, packageBin)).value, file("extracted"))
