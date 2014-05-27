package com.typesafe.sbt.web.js

import scala.annotation.implicitNotFound
import scala.language.implicitConversions

case class JavaScript(raw: String)

class JS[A](val value: A)(implicit write: JS.Write[A]) {
  def get: A = value
  lazy val js: String = write(value)
  override def toString: String = js
}

object JS {
  implicit def apply[A: Write](value: A): JS[A] = new JS(value)

  val undefined: JS[JavaScript] = JS(JavaScript("undefined"))

  class Array(seq: Seq[JS[_]]) extends JS[Seq[JS[_]]](seq) {
    def +(js: JS[_]): Array = new Array(value :+ js)
    def ++(other: Array): Array = new Array(value ++ other.value)
  }

  object Array {
    val empty: Array = apply()
    def apply(values: JS[_]*): Array = new Array(values)
  }

  class Object(map: Map[String, JS[_]]) extends JS[Map[String, JS[_]]](map) {
    def +(property: (String, JS[_])): Object = new Object(value + property)
    def ++(other: Object): Object = new Object(value ++ other.value)
  }

  object Object {
    val empty: Object = apply()
    def apply(properties: (String, JS[_])*): Object = new Object(properties.toMap)
  }

  def write[A: Write](a: A): String = implicitly[Write[A]].apply(a)

  @implicitNotFound("No implicit JavaScript writer found for JS.Write[${A}]")
  trait Write[-A] {
    def apply(a: A): String
  }

  object Write {
    def apply[A](write: A => String) = new Write[A] {
      def apply(a: A): String = write(a)
    }

    implicit val javascript: Write[JavaScript] = Write[JavaScript](_.raw)

    implicit val boolean: Write[Boolean] = Write[Boolean](_.toString)

    implicit val int: Write[Int] = Write[Int](_.toString)

    implicit val long: Write[Long] = Write[Long](_.toString)

    implicit val float: Write[Float] = Write[Float](_.toString)

    implicit val double: Write[Double] = Write[Double](_.toString)

    implicit val string: Write[String] = Write[String](quoted)

    implicit val file: Write[java.io.File] =
      Write[java.io.File](f => quoted(f.getPath))

    implicit val uri: Write[java.net.URI] =
      Write[java.net.URI](u => quoted(u.toString))

    implicit val url: Write[java.net.URL] =
      Write[java.net.URL](u => quoted(u.toString))

    implicit def option[A: Write]: Write[Option[A]] =
      Write[Option[A]](o => o.fold("undefined")(a => write[A](a)))

    implicit def seq[A: Write]: Write[Seq[A]] =
      Write[Seq[A]](s => jsArray(s.map(a => write[A](a))))

    implicit def tuple2[A: Write, B: Write]: Write[(A, B)] =
      Write[(A, B)](t => jsArray(Seq(write[A](t._1), write[B](t._2))))

    implicit def tuple3[A: Write, B: Write, C: Write]: Write[(A, B, C)] =
      Write[(A, B, C)](t => jsArray(Seq(write[A](t._1), write[B](t._2), write[C](t._3))))

    implicit def tuple4[A: Write, B: Write, C: Write, D: Write]: Write[(A, B, C, D)] =
      Write[(A, B, C, D)](t => jsArray(Seq(write[A](t._1), write[B](t._2), write[C](t._3), write[D](t._4))))

    implicit def tuple5[A: Write, B: Write, C: Write, D: Write, E: Write]: Write[(A, B, C, D, E)] =
      Write[(A, B, C, D, E)](t => jsArray(Seq(write[A](t._1), write[B](t._2), write[C](t._3), write[D](t._4), write[E](t._5))))

    implicit def map[V: Write]: Write[Map[String, V]] =
      Write[Map[String, V]](m => jsObject(m.map { case (k, v) => (k, write[V](v)) }))

    implicit def js[J <: JS[_]]: Write[J] = Write[J](_.js)

    def quoted(s: String): String = "\"" + (s flatMap escaped) + "\""

    def escaped(char: Char): String = char match {
      case '"'  => "\\\""
      case '\\' => "\\\\"
      case '\b' => "\\b"
      case '\f' => "\\f"
      case '\n' => "\\n"
      case '\r' => "\\r"
      case '\t' => "\\t"
      case c if c.isControl => f"\\u${c}%04x"
      case c => c.toString
    }

    def jsArray(values: Seq[String]): String = values.mkString("[", ", ", "]")

    def jsObject(properties: Map[String, String]): String =
      properties.map { case (k, v) => quoted(k) + ": " + v }.mkString("{", ", ", "}")
  }
}
