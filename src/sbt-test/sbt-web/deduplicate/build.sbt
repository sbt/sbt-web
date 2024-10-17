lazy val root = (project in file(".")).enablePlugins(SbtWeb)

Assets / WebKeys.deduplicators += SbtWeb.selectFileFrom((Assets / sourceDirectory).value)

TaskKey[Unit]("fileCheck") := {
  assert(( target.value / "web" / "public" / "main" / "js" / "a.js" ).exists(), "Could not find 'web/public/main/js/a.js'")
}