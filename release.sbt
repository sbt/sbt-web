import sbtrelease._
import ReleaseStateTransformations._
import ReleaseKeys._

releaseSettings

lazy val scriptedKey = taskKey[Unit]("scripted")

scriptedKey := {
  val log = streams.value.log
  log.info("Executing scripted...")
  val _ = scripted.toTask("").value
  log.info("...scripted Done!")
}

lazy val runScripted: ReleaseStep = releaseTask(scriptedKey in ThisProject)

releaseProcess := Seq[ReleaseStep](
  checkSnapshotDependencies,
  inquireVersions,
  runClean,
  runTest,
  runScripted,
  setReleaseVersion,
  commitReleaseVersion,
  tagRelease,
  publishArtifacts,
  setNextVersion,
  commitNextVersion,
  pushChanges
)
