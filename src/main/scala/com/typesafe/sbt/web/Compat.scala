package com.typesafe.sbt.web

import sbt._
import sbt.internal.io.Source

object Compat {

  def addWatchSources(
    unmanagedSourcesKey: TaskKey[Seq[File]],
    unmanagedSourceDirectoriesKey: SettingKey[Seq[File]],
    scopeKey: Configuration
  ) = {
    Keys.watchSources ++= {
      val include = (scopeKey / unmanagedSourcesKey / Keys.includeFilter).value
      val exclude = (scopeKey / unmanagedSourcesKey / Keys.excludeFilter).value

      (scopeKey / unmanagedSourceDirectoriesKey).value.map { directory =>
        new Source(directory, include, exclude)
      }
    }
  }

  type CacheStore = sbt.util.CacheStore
  def cacheStore(stream: Keys.TaskStreams, identifier: String): CacheStore = stream.cacheStoreFactory.make(identifier)
}
