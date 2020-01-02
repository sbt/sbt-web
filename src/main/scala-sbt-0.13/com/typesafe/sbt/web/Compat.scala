package com.typesafe.sbt.web

import sbt._

object Compat {

  val Analysis = sbt.inc.Analysis

  def addWatchSources(
    unmanagedSourcesKey: TaskKey[Seq[File]],
    unmanagedSourceDirectoriesKey: SettingKey[Seq[File]],
    scopeKey: Configuration
  ) = {
    Keys.watchSources ++= (unmanagedSourcesKey in scopeKey).value
  }

  type CacheStore = File

  def cacheStore(stream: Keys.TaskStreams, identifier: String): CacheStore = stream.cacheDirectory / identifier

  def sync(store: CacheStore): Traversable[(File, File)] => Relation[File, File] = Sync(store)
}
