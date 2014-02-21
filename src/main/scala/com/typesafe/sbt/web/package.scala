package com.typesafe.sbt

import sbt.File

package object web {
  /**
   * Describes a string path relative to a base directory.
   */
  type PathMappings = Seq[(File, String)]

  /**
   * Maps a source file to a target file.
   */
  type FileMappings = Seq[(File, File)]
}
