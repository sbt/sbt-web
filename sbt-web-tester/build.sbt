val root = (project in file(".")).addPlugins(SbtWeb)

libraryDependencies ++= Seq(
  "org.webjars" % "jquery" % "2.0.3-1"
)
