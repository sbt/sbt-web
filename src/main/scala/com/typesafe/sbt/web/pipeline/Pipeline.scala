package com.typesafe.sbt.web.pipeline

import sbt._
import sbt.Def.Initialize

object Pipeline {
  /**
   * Mappings are used in the asset pipeline, with the string path relative to a base directory.
   */
  type Mappings = Seq[(File, String)]

  /**
   * Each pipeline stage transforms the mappings from the previous stage.
   */
  type Stage = Mappings => Mappings

  /**
   * Dynamically compose a sequence of transforming function tasks into a single task.
   */
  def apply[A](stages: Seq[Task[A => A]], functions: Seq[A => A] = Seq.empty): Initialize[Task[A => A]] = {
    if (stages.isEmpty) Def.task { Function.chain(functions) }
    else Def.taskDyn { apply(stages.tail, functions :+ stages.head.value) }
  }

  /**
   * Dynamically create a chained pipeline task from a setting that contains the stages.
   */
  def chain[A](stagesKey: SettingKey[Seq[Task[A => A]]]): Initialize[Task[A => A]] =
    Def.taskDyn { Pipeline(stagesKey.value) }

  /**
   * Filter for non-directory files.
   */
  val isNotDirectory: FileFilter = new SimpleFileFilter(!_.isDirectory)

  /**
   * Create mappings for all non-directory files under a base directory.
   */
  def mappings(dir: File): Mappings = Path.selectSubpaths(dir, isNotDirectory).toSeq
}
