package com.typesafe.sbt

import sbt.File

package object web {
  /**
   * Describes a string path relative to a base directory.
   */
  type PathMapping = (File, String)

}
