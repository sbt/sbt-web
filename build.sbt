lazy val `sbt-web` = (project in file(".")).enablePlugins(SbtWebBase)

description := "sbt web support"

libraryDependencies ++= Seq(
  "org.webjars" % "webjars-locator-core" % "0.52",
  "org.specs2" %% "specs2-core" % "4.20.0" % "test",
  "junit" % "junit" % "4.13.2" % "test"
)
