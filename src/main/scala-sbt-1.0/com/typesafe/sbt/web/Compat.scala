package com.typesafe.sbt.web

import sbt._
import sbt.internal.io.Source
import akka.actor.ActorSystem

object Compat {

  val Analysis = sbt.internal.inc.Analysis

  def addWatchSources(
    unmanagedSourcesKey: TaskKey[Seq[File]],
    unmanagedSourceDirectoriesKey: SettingKey[Seq[File]],
    scopeKey: Configuration
  ) = {
    Keys.watchSources ++= {
      val include = (Keys.includeFilter in unmanagedSourcesKey in scopeKey).value
      val exclude = (Keys.excludeFilter in unmanagedSourcesKey in scopeKey).value

      (unmanagedSourceDirectoriesKey in scopeKey).value.map { directory =>
        new Source(directory, include, exclude)
      }
    }
  }

  type CacheStore = sbt.util.CacheStore
  def cacheStore(stream: Keys.TaskStreams, identifier: String): CacheStore = stream.cacheStoreFactory.make(identifier)

  def terminateActorSystem(actorSystem: ActorSystem) = actorSystem.terminate()

  def sync(store: CacheStore): Traversable[(File, File)] => Relation[File, File] = Sync.sync(store)
}
