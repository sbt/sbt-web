addSbtPlugin("com.github.sbt" % "sbt-web" % sys.props("project.version"))
Compile / scalacOptions += "-Xmacro-settings:sbt:no-default-task-cache"
