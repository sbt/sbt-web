lazy val root = (project in file(".")).enablePlugins(SbtWeb)

libraryDependencies ++= Seq(
  "org.webjars" % "jquery"    % "2.0.3-1",
  "org.webjars" % "prototype" % "1.7.1.0",
  "org.webjars" % "less-node" % "1.6.0-1",
  "org.webjars" % "requirejs" % "2.1.11-1" % "test"
)
