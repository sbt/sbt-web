organization := "com.typesafe.sbt"
name := "sbt-web"
description := "sbt web support"
homepage := Some(url("https://github.com/sbt/sbt-web"))
licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html"))

sbtPlugin := true
scalaVersion := "2.10.4"

scalacOptions ++= Seq("-deprecation", "-feature")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "org.webjars" % "webjars-locator" % "0.21",
  "org.specs2" %% "specs2-core" % "3.4" % "test",
  "junit" % "junit" % "4.11" % "test"
)
// Required by specs2 to get scalaz-stream
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

scriptedSettings
scriptedLaunchOpts += "-Dproject.version=" + version.value
