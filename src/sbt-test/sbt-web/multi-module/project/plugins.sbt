addSbtPlugin("com.typesafe.sbt" % "sbt-web" % sys.props("project.version"))

resolvers ++= Seq(
  Resolver.sbtPluginRepo("snapshots"),
  Resolver.sonatypeRepo("snapshots"),
  Resolver.typesafeRepo("snapshots")
)
