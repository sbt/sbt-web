lazy val root = (project in file(".")).enablePlugins(SbtWeb)

name := "web-project"

version := "0.1"

crossPaths := false

libraryDependencies += "org.webjars" % "jquery" % "2.0.3-1"

WebKeys.packagePrefix in Assets := "public/"

TaskKey[Unit]("extractAssets") := IO.unzip((artifactPath in (Assets, packageBin)).value, file("extracted"))
