lazy val a = project in file(".")

lazy val b = (project dependsOn(a)).enablePlugins(SbtWeb)

mappings in (Compile, packageBin) += (baseDirectory.value / "src" / "main" / "js" / "a.js", "META-INF/resources/webjars/a/1/a.js")

exportJars := true