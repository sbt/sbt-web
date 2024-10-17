lazy val root = (project in file(".")).enablePlugins(SbtWeb)

TaskKey[Unit]("fileCheck") := {
  assert(!( target.value / "web" / "public" / "main" / ".svn" / "entries" ).exists(), "Found private directory, which should have been excluded.")
}
