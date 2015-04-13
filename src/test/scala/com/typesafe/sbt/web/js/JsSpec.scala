package com.typesafe.sbt.web.js

import org.specs2.mutable.Specification

class JsSpec extends Specification {

  "JS" should {

    "create JS objects from various types" in {
      JS(1).js must_== "1"
      JS(true).js must_== "true"
      JS("a").js must_== "\"a\""
      JS("ab\tc").js must_== "\"ab\\tc\""
      JS(Seq(1, 2, 42)).js must_== "[1, 2, 42]"
      JS(Map("a" -> "b")).js must_== """{"a": "b"}"""
      JS(new java.io.File("/path/to/file")).js must_== "\"/path/to/file\""
      JS(1 to 3).js must_== "[1, 2, 3]"
      JS((1, "b", Map("a" -> 42))).js must_== """[1, "b", {"a": 42}]"""
      JS.Array(1, "b", Map("a" -> 42)).js must_== """[1, "b", {"a": 42}]"""
      JS(Some(1)).js must_== "1"
      JS(None: Option[Int]).js must_== "undefined"
      JS(JavaScript("function f() { ... }")).js must_== "function f() { ... }"
      JS.undefined.js must_== "undefined"
    }

    "create JS objects with implicit conversions" in {
      val config = JS.Object(
        "string"   -> "abc",
        "seq"      -> Seq(1, 2, 42),
        "map"      -> Map("answer" -> 42),
        "range"    -> (1 to 3),
        "mixed"    -> (1, "b", Map("a" -> 42)),
        "file"     -> new java.io.File("/path/to/file"),
        "function" -> JavaScript("function f() { ... }")
      )

      config.get("string").js must_== "\"abc\""
      config.get("seq").js must_== "[1, 2, 42]"
      config.get("map").js must_== """{"answer": 42}"""
      config.get("range").js must_== "[1, 2, 3]"
      config.get("mixed").js must_== """[1, "b", {"a": 42}]"""
      config.get("file").js must_== "\"/path/to/file\""
      config.get("function").js must_== "function f() { ... }"
    }

  }

}
