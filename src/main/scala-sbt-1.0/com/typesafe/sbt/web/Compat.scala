package com.typesafe.sbt.web

object Compat {

  // This method (and the `Compat` object) is only kept alive to allow Pekko v1.x to use
  // sbt-paradox-material-theme v0.7.x which still depends on paradox 0.9.2 (which is the one that does access
  // the `cacheStore` method below). This method here (and `Compat`) will be removed once Pekko 1.x is EOL and
  // Pekko 2.x upgraded to a newer sbt-paradox-material-theme release.
  //
  // More details:
  // [[https://github.com/sbt/sbt-paradox-material-theme/pull/61]]
  /**
   * @deprecated
   *   Use `streams.value.cacheStoreFactory.make("...")` instead
   */
  @deprecated(message = """Use `streams.value.cacheStoreFactory.make("...")` instead""")
  def cacheStore(stream: sbt.Keys.TaskStreams, identifier: String): sbt.util.CacheStore =
    stream.cacheStoreFactory.make(identifier)
}
