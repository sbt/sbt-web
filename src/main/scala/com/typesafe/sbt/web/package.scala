package com.typesafe.sbt

import java.io.File

package object web {
  /**
   * Describes a string path relative to a base directory.
   */
  type PathMapping = (File, String)

}
