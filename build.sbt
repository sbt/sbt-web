sbtPlugin := true

organization := "com.typesafe.sbt"

name := "sbt-web"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-deprecation", "-feature")

resolvers += Resolver.sonatypeRepo("snapshots")

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "org.webjars" % "webjars-locator" % "0.12",
  "org.specs2" %% "specs2" % "2.2.2" % "test",
  "junit" % "junit" % "4.11" % "test"
  )

// FIXME: Working around https://github.com/sbt/sbt/issues/1156#issuecomment-39317363
isSnapshot := true

publishMavenStyle := false

publishTo := {
    val isSnapshot = version.value.contains("-SNAPSHOT")
    val scalasbt = "http://repo.scala-sbt.org/scalasbt/"
    val (name, url) = if (isSnapshot)
                        ("sbt-plugin-snapshots", scalasbt + "sbt-plugin-snapshots")
                      else
                        ("sbt-plugin-releases", scalasbt + "sbt-plugin-releases")
    Some(Resolver.url(name, new URL(url))(Resolver.ivyStylePatterns))
}

scriptedSettings

scriptedLaunchOpts <+= version apply { v => s"-Dproject.version=$v" }
