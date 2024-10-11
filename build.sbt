lazy val `sbt-web` = (project in file(".")).enablePlugins(SbtWebBase)

sonatypeProfileName := "com.github.sbt.sbt-web" // See https://issues.sonatype.org/browse/OSSRH-77819#comment-1203625

description := "sbt web support"

developers += Developer(
  "playframework",
  "The Play Framework Team",
  "contact@playframework.com",
  url("https://github.com/playframework")
)

lazy val scala212 = "2.12.20"
lazy val scala3 = "3.3.4"
ThisBuild / crossScalaVersions := Seq(scala212, scala3)

libraryDependencies ++= Seq(
  "org.webjars" % "webjars-locator-core" % "0.59",
  "org.specs2" %% "specs2-core"          % "4.20.8" % "test",
  "junit"       % "junit"                % "4.13.2" % "test"
)

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
ThisBuild / dynverVTagPrefix := false

// Sanity-check: assert that version comes from a tag (e.g. not a too-shallow clone)
// https://github.com/dwijnand/sbt-dynver/#sanity-checking-the-version
Global / onLoad := (Global / onLoad).value.andThen { s =>
  dynverAssertTagVersion.value
  s
}

(pluginCrossBuild / sbtVersion) := {
  scalaBinaryVersion.value match {
    case "2.12" => "1.10.2"
    case _      => "2.0.0-M2"
  }
}
