lazy val a = (project in file("module/a"))
  .enablePlugins(SbtWeb)
  .settings(
    name := "Web Module A",
    organization := "com.github.sbt.web.test",
    version := "0.1-SNAPSHOT",
    libraryDependencies += "org.webjars" % "jquery" % "2.0.3-1"
  )

lazy val b = (project in file("module/b"))
  .enablePlugins(SbtWeb)
  .settings(
    name := "Web Module B",
    organization := "com.github.sbt.web.test",
    version := "0.1-SNAPSHOT",
    libraryDependencies += "com.github.sbt.web.test" %% "web-module-a" % "0.1-SNAPSHOT"
  )

lazy val c = (project in file("."))
  .enablePlugins(SbtWeb)
  .settings(
    libraryDependencies += "com.github.sbt.web.test" %% "web-module-b" % "0.1-SNAPSHOT"
  )
