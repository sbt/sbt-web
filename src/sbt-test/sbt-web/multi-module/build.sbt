import FileAssertions.*

lazy val a = (project in file("."))
  .enablePlugins(SbtWeb)
  .dependsOn(b)

lazy val b = (project in file("modules/b"))
  .enablePlugins(SbtWeb)
  .dependsOn(c % "compile;test->test", d % "compile;test->test")
  .settings(
    (TestAssets / WebKeys.directWebModules) := Nil
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

// Check for files

//$ exists target/web/public/main/js/a.js
//$ exists target/web/public/main/lib/b/js/b.js
//$ exists target/web/public/main/lib/c/js/c.js
//$ exists target/web/public/main/lib/d/js/d.js
//$ exists target/web/public/main/lib/e/js/e.js
//$ exists target/web/public/main/lib/jquery/jquery.js
TaskKey[Unit]("fileCheckA") := {
  assertLibrary(target.value, "a", Root())
  assertLibrary(target.value, "b")
  assertLibrary(target.value, "c")
  assertLibrary(target.value, "d")
  assertLibrary(target.value, "e")
  assertLibrary(target.value, "jquery", External())
}

//$ exists modules/b/target/web/public/main/js/b.js
//$ exists modules/b/target/web/public/main/lib/c/js/c.js
//$ exists modules/b/target/web/public/main/lib/d/js/d.js
//$ exists modules/b/target/web/public/main/lib/e/js/e.js
//$ exists modules/b/target/web/public/main/lib/jquery/jquery.js
TaskKey[Unit]("fileCheckB") := {
  assertLibrary((b / target).value , "b", Root())
  assertLibrary((b / target).value, "c")
  assertLibrary((b / target).value, "d")
  assertLibrary((b / target).value, "e")
  assertLibrary((b / target).value, "jquery", External())
}

//$ exists modules/c/target/web/public/main/js/c.js
//$ exists modules/c/target/web/public/main/js/e.js
//$ exists modules/c/target/web/public/main/lib/jquery/jquery.js
TaskKey[Unit]("fileCheckC") := {
  assertLibrary((c / target).value, "c", Root())
  assertLibrary((c / target).value, "e", Root())
  assertLibrary((c / target).value, "jquery", External())
}

//$ exists modules/d/target/web/public/main/js/d.js
//$ exists modules/d/target/web/public/main/lib/e/js/e.js
//$ exists modules/d/target/web/public/main/lib/jquery/jquery.js
TaskKey[Unit]("fileCheckD") := {
  assertLibrary((d / target).value, "d", Root())
  assertLibrary((d / target).value, "e")
  assertLibrary((d / target).value, "jquery", External())
}

//$ exists modules/e/target/web/public/main/js/e.js
//$ exists modules/e/target/web/public/main/lib/jquery/jquery.js
TaskKey[Unit]("fileCheckE") := {
  assertLibrary((e / target).value, "e", Root())
  assertLibrary((e / target).value, "jquery", External())
}

//$ exists modules/b/target/web/public/test/lib/c/js/c.js
//$ exists modules/b/target/web/public/test/lib/c/js/u.js
//$ exists modules/b/target/web/public/test/lib/d/js/d.js
//$ exists modules/b/target/web/public/test/lib/d/js/u.js
//$ exists modules/b/target/web/public/test/lib/e/js/e.js
//$ exists modules/b/target/web/public/test/lib/e/js/t.js
//$ exists modules/b/target/web/public/test/lib/jquery/jquery.js
TaskKey[Unit]("fileCheckBTest") := {
  assertLibrary((b / target).value, "b", Library("test"))
  assertLibrary((b / target).value, "c", Library("test"))
  assertLibrary((b / target).value, "c", Library("test"), Some("u"))
  assertLibrary((b / target).value, "d", Library("test"))
  assertLibrary((b / target).value, "d", Library("test"), Some("u"))
  assertLibrary((b / target).value, "e", Library("test"))
  assertLibrary((b / target).value, "e", Library("test"), Some("t"))
  assertLibrary((b / target).value, "jquery", External("test"))
}

//# c has set import directly
//$ exists modules/c/target/web/public/test/js/c.js
//$ exists modules/c/target/web/public/test/js/u.js
//$ exists modules/c/target/web/public/test/js/e.js
//$ exists modules/c/target/web/public/test/js/t.js
//$ exists modules/c/target/web/public/test/lib/jquery/jquery.js
TaskKey[Unit]("fileCheckCTest") := {
  assertLibrary((c / target).value, "c", Root("test"))
  assertLibrary((c / target).value, "c", Root("test"), Some("u"))
  assertLibrary((c / target).value, "e", Root("test"))
  assertLibrary((c / target).value, "e", Root("test"), Some("t"))
  assertLibrary((c / target).value, "jquery", External("test"))
}

//$ exists modules/d/target/web/public/test/js/d.js
//$ exists modules/d/target/web/public/test/js/u.js
//$ exists modules/d/target/web/public/test/lib/e/js/e.js
//$ exists modules/d/target/web/public/test/lib/e/js/t.js
//$ exists modules/d/target/web/public/test/lib/jquery/jquery.js
TaskKey[Unit]("fileCheckDTest") := {
  assertLibrary((d / target).value, "d", Root("test"))
  assertLibrary((d / target).value, "d", Root("test"), Some("u"))
  assertLibrary((d / target).value, "e", Library("test"))
  assertLibrary((d / target).value, "e", Library("test"), Some("t"))
  assertLibrary((d / target).value, "jquery", External("test"))
}

//$ exists modules/e/target/web/public/test/js/e.js
//$ exists modules/e/target/web/public/test/js/t.js
//$ exists modules/e/target/web/public/test/lib/jquery/jquery.js
TaskKey[Unit]("fileCheckETest") := {
  assertLibrary((e / target).value, "e", Root("test"))
  assertLibrary((e / target).value, "e", Root("test"), Some("t"))
  assertLibrary((e / target).value, "jquery", External("test"))
}

//$ exists target/web/public/main/lib/e/js/e.js
TaskKey[Unit]("fileCheckATracked") := {
  assertLibrary(target.value, "e")
}

//$ exists target/web/public/main/lib/e/js/e.js
TaskKey[Unit]("fileCheckETracked") := {
  assertLibrary(target.value, "e")
}
