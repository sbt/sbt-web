package com.typesafe.sbt.web

import akka.actor.ActorSystem
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

  def terminateActorSystem(actorSystem: ActorSystem) = actorSystem.shutdown()
}
