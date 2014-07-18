lazy val module = (project in file("module"))
  .enablePlugins(SbtWeb)
  .settings(
    name := "Web Module",
    organization := "com.typesafe.sbt.web.test",
    version := "0.1-SNAPSHOT",
    crossPaths := false,
    libraryDependencies += "org.webjars" % "jquery" % "2.0.3-1",
    publishArtifact in Assets := true
  )

lazy val other = (project in file("other"))
  .enablePlugins(SbtWeb)
  .settings(
    libraryDependencies += "com.typesafe.sbt.web.test" % "web-module" % "0.1-SNAPSHOT" classifier "web-assets"
  )
