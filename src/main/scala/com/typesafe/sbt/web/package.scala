package com.typesafe.sbt

import PluginCompat.FileRef

import java.io.File

package object web {

  /**
   * Describes a string path relative to a base directory.
   */
  type PathMapping = (FileRef, String)

  /**
   * A function for possibly selecting a single file from a sequence.
   */
  type Deduplicator = Seq[File] => Option[File]
}
