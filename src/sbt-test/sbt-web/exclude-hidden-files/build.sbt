lazy val root = (project in file(".")).enablePlugins(SbtWeb)

TaskKey[Unit]("fileCheck") := {
  assert(
    !(target.value / "web" / "public" / "main" / ".svn" / "entries").exists(),
    "Found private directory, which should have been excluded."
  )
}

ThisBuild / scalaVersion := {
  if (sbtVersion.value.startsWith("1.")) "2.12.21" else "3.9.0"
}
