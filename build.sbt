lazy val `sbt-web` = project in file(".")

description := "sbt web support"

def pickVersion(scalaVer: String, default: String, forScala210: String): String = CrossVersion.binaryScalaVersion(scalaVer) match {
  case "2.10" => forScala210
  case _ => default
}

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % pickVersion(scalaVersion.value, default = "2.5.17", forScala210 = "2.3.16"),
  "org.webjars" % "webjars-locator-core" % "0.36",
  "org.specs2" %% "specs2-core" % pickVersion(scalaVersion.value, default = "4.3.5", forScala210 = "3.10.0") % "test",
  "junit" % "junit" % "4.12" % "test"
)
