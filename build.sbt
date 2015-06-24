lazy val `sbt-web` = project in file(".")

description := "sbt web support"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "org.webjars" % "webjars-locator" % "0.26",
  "org.specs2" %% "specs2-core" % "3.4" % "test",
  "junit" % "junit" % "4.12" % "test"
)
// Required by specs2 to get scalaz-stream
resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"
