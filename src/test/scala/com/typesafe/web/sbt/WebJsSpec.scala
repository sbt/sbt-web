package com.typesafe.web.sbt

import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import org.specs2.mutable.Specification
import com.typesafe.sbt.web.SbtWeb.autoImport.WebJs._

@RunWith(classOf[JUnitRunner])
class WebJsSpec extends Specification {

  "WebJs" should {

    "create JS objects from various types" in {
      JS(1).v must_== "1"
      JS("2").v must_== "2"
      j"1".v must_== "\"1\""
      Map("a" -> j"b").toJS.v must_== """{"a":"b"}"""
      Seq(j"b").toJS.v must_== """["b"]"""
    }

  }

}