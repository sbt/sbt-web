import com.typesafe.sbt.web.SbtWebPlugin

lazy val a = project.in(file("."))

lazy val b = project.dependsOn(a).addPlugins(SbtWebPlugin)

mappings in (Compile, packageBin) += (baseDirectory.value / "src" / "main" / "js" / "a.js", "META-INF/resources/webjars/a/1/a.js")

exportJars := true