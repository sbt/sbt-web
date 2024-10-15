lazy val a = (project in file("."))
  .enablePlugins(SbtWeb)
  .dependsOn(b)

lazy val b = (project in file("modules/b"))
  .enablePlugins(SbtWeb)
  .dependsOn(c % "compile;test->test", d % "compile;test->test")
  .settings(
    TestAssets / WebKeys.directWebModules := Nil
  )

lazy val c = (project in file("modules/c"))
  .enablePlugins(SbtWeb)
  .dependsOn(e % "compile;test->test", x)
  .settings(
    WebKeys.importDirectly := true
  )

lazy val d = (project in file("modules/d"))
  .enablePlugins(SbtWeb)
  .dependsOn(e % "compile;test->test", x)

lazy val e = (project in file("modules/e"))
  .enablePlugins(SbtWeb)
  .settings(
    libraryDependencies += "org.webjars" % "jquery" % "2.0.3-1"
  )

lazy val x = (project in file("modules/x"))
