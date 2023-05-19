addSbtPlugin("com.github.sbt" % "sbt-web" % sys.props("project.version"))

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
)
