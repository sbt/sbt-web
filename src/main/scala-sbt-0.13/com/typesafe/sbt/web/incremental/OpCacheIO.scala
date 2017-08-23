/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.sbt.web.incremental

import java.io.File

import sbinary.Operations._
import sbinary._
import sbt.CacheIO

import scala.collection.immutable.{Map, Set}

/**
 * Support for reading and writing cache files.
 */
private[incremental] object OpCacheIO {

  import OpCacheProtocol.OpCacheFormatV2

  def toFile(cache: OpCache, file: File): Unit = {
    CacheIO.toFile(OpCacheFormatV2)(cache)(file)
  }

  def fromFile(file: File): OpCache = {
    CacheIO.fromFile(OpCacheFormatV2, new OpCache())(file)
  }

}

/**
 * Binary formats for cache files.
 */
private[incremental] object OpCacheProtocol extends DefaultProtocol {

  import OpCache.{FileHash, Record}

  implicit def cacheContentFormat: Format[Map[OpInputHash, Record]] =
    immutableMapFormat[OpInputHash, Record](OpInputHashFormat, RecordFormat)

  implicit def fileDepsFormat: Format[Set[FileHash]] =
    immutableSetFormat[FileHash](FileHashFormat)

  /**
   * SBT's CacheIO stores a hash of the format type in the cache. By including a version number in the name, and
   * incrementing that version number each time the format changes, this ensures that when the version changes,
   * SBT won't try and load the cache, but will treat it as if there is no cache.
   */
  implicit object OpCacheFormatV2 extends Format[OpCache] {
    def reads(in: Input): OpCache = new OpCache(read[Map[OpInputHash, Record]](in))
    def writes(out: Output, oc: OpCache) = write[Map[OpInputHash, Record]](out, oc.content)
  }

  implicit object BytesFormat extends Format[Bytes] {
    def reads(in: Input): Bytes = Bytes(read[Array[Byte]](in))
    def writes(out: Output, bytes: Bytes) = write[Array[Byte]](out, bytes.arr)
  }

  implicit object OpInputHashFormat extends Format[OpInputHash] {
    def reads(in: Input): OpInputHash = OpInputHash(read[Bytes](in))
    def writes(out: Output, oih: OpInputHash) = write[Bytes](out, oih.bytes)
  }

  implicit object RecordFormat extends Format[Record] {
    def reads(in: Input): Record = Record(read[Set[FileHash]](in), read[Set[File]](in))
    def writes(out: Output, r: Record) = {
      write[Set[FileHash]](out, r.fileHashes)
      write[Set[File]](out, r.products)
    }
  }

  implicit object FileHashFormat extends Format[FileHash] {
    def reads(in: Input): FileHash = FileHash(read[File](in), read[Option[Bytes]](in))
    def writes(out: Output, fh: FileHash) = {
      write[File](out, fh.file)
      write[Option[Bytes]](out, fh.sha1IfExists)
    }
  }

}
