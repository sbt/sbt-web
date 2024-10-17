import FileAssertions.*

lazy val root = (project in file(".")).enablePlugins(SbtWeb)

name := "Web Project"

version := "0.1"

crossPaths := false

libraryDependencies += "org.webjars" % "jquery" % "2.0.3-1"

TaskKey[Unit]("extractAssets") := IO.unzip(
  SbtWeb.asFile(((Assets / packageBin) / artifactPath).value, fileConverter.value),
  file("extracted")
)

// $ exists target/web-project-0.1-web-assets.jar
TaskKey[Unit]("fileCheckAssets") := {
  assertExists(target.value / "web-project-0.1-web-assets.jar")
}

// $ exists extracted/js/a.js
// $ exists extracted/lib/jquery/jquery.js
TaskKey[Unit]("fileCheckExtracted") := {
  assertExists(baseDirectory.value / "extracted" / "js" / "a.js")
  assertExists(baseDirectory.value / "extracted" / "lib" / "jquery" / "jquery.js")
}

// $ exists extracted/public/js/a.js
// $ exists extracted/public/lib/jquery/jquery.js
TaskKey[Unit]("fileCheckPublic") := {
  assertExists(baseDirectory.value / "extracted" / "public" / "js" / "a.js")
  assertExists(baseDirectory.value / "extracted" / "public" / "lib" / "jquery" / "jquery.js")
}
