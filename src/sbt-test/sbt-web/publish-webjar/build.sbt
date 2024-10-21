import FileAssertions.*

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

//$ exists target/web/web-modules/main/webjars/lib/jquery/jquery.js
//$ exists target/web/web-modules/main/webjars/lib/web-module-a/js/a.js
//$ exists target/web/web-modules/main/webjars/lib/web-module-b/js/b.js
//
//$ exists target/web/public/main/lib/jquery/jquery.js
//$ exists target/web/public/main/lib/web-module-a/js/a.js
//$ exists target/web/public/main/lib/web-module-b/js/b.js
TaskKey[Unit]("fileCheck") := {
  assertExists(target.value / "web" / "web-modules" / "main" / "webjars" / "lib" / "jquery" / "jquery.js")
  assertExists(target.value / "web" / "web-modules" / "main" / "webjars" / "lib" / "web-module-a" / "js" / "a.js")
  assertExists(target.value / "web" / "web-modules" / "main" / "webjars" / "lib" / "web-module-b" / "js" / "b.js")
  assertExists(target.value / "web" / "public" / "main" / "lib" / "jquery" / "jquery.js")
  assertExists(target.value / "web" / "public" / "main" / "lib" / "web-module-a" / "js" / "a.js")
  assertExists(target.value / "web" / "public" / "main" / "lib" / "web-module-b" / "js" / "b.js")
}
