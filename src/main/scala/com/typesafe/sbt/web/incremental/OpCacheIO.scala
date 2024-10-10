/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.sbt.web.incremental

import java.io.File
import java.util.Base64

import sbt.util.CacheStore

import scala.collection.immutable.{ Map, Set }

/**
 * Support for reading and writing cache files.
 */
private[incremental] object OpCacheIO {

  import OpCacheProtocol.opCacheFormat

  def toFile(cache: OpCache, file: File): Unit = {
    CacheStore(file).write(cache)
  }

  def fromFile(file: File): OpCache = {
    CacheStore(file).read(new OpCache())
  }
}

/**
 * Binary formats for cache files.
 */
private[incremental] object OpCacheProtocol {

  import sjsonnew.*
  import BasicJsonProtocol.*

  import OpCache.{ FileHash, Record }

  implicit val fileFormat: JsonFormat[File] = BasicJsonProtocol.projectFormat[File, String](_.getAbsolutePath, new File(_))
  implicit val bytesFormat: JsonFormat[Bytes] = BasicJsonProtocol.projectFormat[Bytes, String](
    bytes => Base64.getEncoder.encodeToString(bytes.arr),
    bytes => new Bytes(Base64.getDecoder.decode(bytes))
  )
  implicit val opInputHashKeyFormat: JsonKeyFormat[OpInputHash] = JsonKeyFormat[OpInputHash](
    hash => Base64.getEncoder.encodeToString(hash.bytes.arr),
    hashBytes => OpInputHash(Bytes(Base64.getDecoder.decode(hashBytes)))
  )

  implicit object FileHashFormat extends JsonFormat[FileHash] {
    def write[J](hash: FileHash, builder: Builder[J]): Unit = {
      builder.beginObject()
      builder.addField("file", hash.file)
      builder.addField("sha1IfExists", hash.sha1IfExists)
      builder.endObject()
    }
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): FileHash =
      jsOpt match {
        case Some(js) =>
          unbuilder.beginObject(js)
          val file = unbuilder.readField[File]("file")
          val sha1IfExists = unbuilder.readField[Option[Bytes]]("sha1IfExists")
          unbuilder.endObject()
          FileHash(file, sha1IfExists)
        case None =>
          deserializationError("Expected JsObject but found None")
      }
  }

  implicit object RecordFormat extends JsonFormat[Record] {
    def write[J](record: Record, builder: Builder[J]): Unit = {
      builder.beginObject()
      builder.addField("fileHashes", record.fileHashes)
      builder.addField("products", record.products)
      builder.endObject()
    }
    def read[J](jsOpt: Option[J], unbuilder: Unbuilder[J]): Record =
      jsOpt match {
        case Some(js) =>
          unbuilder.beginObject(js)
          val fileHashes = unbuilder.readField[Set[FileHash]]("fileHashes")
          val products = unbuilder.readField[Set[File]]("products")
          unbuilder.endObject()
          Record(fileHashes, products)
        case None =>
          deserializationError("Expected JsObject but found None")
      }
  }

  implicit val opCacheFormat: JsonFormat[OpCache] = {
    BasicJsonProtocol.projectFormat[OpCache, Map[OpInputHash, Record]](_.content, new OpCache(_))
  }
}
