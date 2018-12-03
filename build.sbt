lazy val `sbt-web` = (project in file(".")).enablePlugins(SbtWebBase)

description := "sbt web support"

def pickVersion(scalaBinaryVersion: String, default: String, forScala210: String): String = scalaBinaryVersion match {
  case "2.10" => forScala210
  case _ => default
}

libraryDependencies ++= Seq(
  "org.webjars" % "webjars-locator-core" % "0.43",
  "org.specs2" %% "specs2-core" % pickVersion(scalaBinaryVersion.value, default = "4.8.1", forScala210 = "3.10.0") % "test",
  "junit" % "junit" % "4.12" % "test"
)
