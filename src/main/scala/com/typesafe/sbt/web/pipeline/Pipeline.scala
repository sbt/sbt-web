package com.typesafe.sbt.web.pipeline

import sbt._
import sbt.Def.Initialize
import com.typesafe.sbt.web.PathMapping

object Pipeline {
  /**
   * Each pipeline stage transforms the mappings from the previous stage.
   */
  type Stage = Seq[PathMapping] => Seq[PathMapping]

  /**
   * Dynamically compose a sequence of transforming function tasks into a single task.
   */
  def apply[A](stages: Seq[TaskKey[A => A]], functions: Seq[A => A] = Seq.empty): Initialize[Task[A => A]] = {
    if (stages.isEmpty) Def.task { Function.chain(functions) }
    else Def.taskDyn { apply(stages.tail, functions :+ stages.head.value) }
  }

  /**
   * Dynamically create a chained pipeline task from a setting that contains the stages.
   */
  def chain[A](stagesKey: SettingKey[Seq[TaskKey[A => A]]]): Initialize[Task[A => A]] =
    Def.taskDyn { Pipeline(stagesKey.value) }

}
