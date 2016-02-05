package com.typesafe.sbt.web

import sbt._
import sbt.Keys.Classpath

object CompatKeys {
  val exportedProductsIfMissing = TaskKey[Classpath]("exported-products-if-missing", "Build products that go on the exported classpath if missing.")
  val exportedProductsNoTracking = TaskKey[Classpath]("exported-products-no-tracking", "Just the exported classpath without triggering the compilation.")
}
