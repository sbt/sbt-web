lazy val root = (project in file(".")).enablePlugins(SbtWeb)

libraryDependencies ++= Seq(
  "org.webjars" % "jquery"    % "2.0.3-1",
  "org.webjars" % "prototype" % "1.7.1.0",
  "org.webjars" % "less-node" % "1.6.0-1",
  "org.webjars" % "requirejs" % "2.1.11-1" % "test"
)

//$ exists target/web/public/main/lib/jquery/jquery.js
//$ exists target/web/public/main/lib/prototype/prototype.js
//-$ exists target/web/public/main/lib/requirejs/require.js
TaskKey[Unit]("fileCheckAssets") := {
  assert(
    ( target.value / "web" / "public" / "main" / "lib" / "jquery" / "jquery.js" ).exists(),
    "Could not find jquery.js"
  )
  assert(
    ( target.value / "web" / "public" / "main" / "lib" / "prototype" / "prototype.js" ).exists(),
    "Could not find prototype.js"
  )
  assert(
    !( target.value / "web" / "public" / "main" / "lib" / "requirejs" / "require.js" ).exists(),
    "Found requirejs, which should have been excluded."
  )

}

//$ exists target/web/node-modules/main/webjars/less/package.json
TaskKey[Unit]("fileCheckNode") := {
  assert(
    ( target.value / "web" / "node-modules" / "main" / "webjars" / "less" / "package.json" ).exists(),
    "Could not find node webjars"
  )
}

//# jquery.js should not have been re-extracted, assert that it is older
//$ newer target/foo target/web/public/main/lib/jquery/jquery.js
TaskKey[Unit]("checkJqueryTimestamp") := {
  assert(
    ( target.value / "web" / "public" / "main" / "lib" / "jquery" / "jquery.js" ).exists(),
    "Could not find jquery"
  )
}

//# All webjars on test classpath are extracted for tests
//$ exists target/web/web-modules/test/webjars/lib/requirejs/require.js
//$ exists target/web/web-modules/test/webjars/lib/jquery/jquery.js
//$ exists target/web/web-modules/test/webjars/lib/prototype/prototype.js
TaskKey[Unit]("checkTestAssets") := {
  assert(
    ( target.value / "web" / "web-modules" / "test" / "webjars" / "lib" / "requirejs" / "require.js" ).exists(),
    "Could not find requirejs webjars in test"
  )
  assert(
    ( target.value / "web" / "web-modules" / "test" / "webjars" / "lib" / "jquery" / "jquery.js" ).exists(),
    "Could not find jquery webjars in test"
  )
  assert(
    ( target.value / "web" / "web-modules" / "test" / "webjars" / "lib" / "prototype" / "prototype.js" ).exists(),
    "Could not find prototype webjars in test"
  )
}

//# Now check everything was aggregated in test assets
//$ exists target/web/public/test/lib/jquery/jquery.js
//$ exists target/web/public/test/lib/prototype/prototype.js
//$ exists target/web/public/test/lib/requirejs/require.js
TaskKey[Unit]("checkTestLibs") := {
  assert(
    ( target.value / "web" / "public" / "test" / "lib" / "requirejs" / "require.js" ).exists(),
    "Could not find requirejs library in test"
  )
  assert(
    ( target.value / "web" / "public" / "test" / "lib" / "jquery" / "jquery.js" ).exists(),
    "Could not find jquery library in test"
  )
  assert(
    ( target.value / "web" / "public" / "test" / "lib" / "prototype" / "prototype.js" ).exists(),
    "Could not find prototype library in test"
  )
}
