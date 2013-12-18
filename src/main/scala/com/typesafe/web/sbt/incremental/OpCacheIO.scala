/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.web.sbt.incremental

import java.io.File
import sbinary._
import sbinary.Operations._
import sbt.{ CacheIO, Hash }
import scala.collection.immutable.{ Map, Seq, Set }

/**
 * Support for reading and writing cache files.
 */
private[incremental] object OpCacheIO {

  import OpCacheProtocol.OpCacheFormat

  def toFile(cache: OpCache, file: File): Unit = {
    CacheIO.toFile(OpCacheFormat)(cache)(file)
  }

  def fromFile(file: File): OpCache = {
    CacheIO.fromFile(OpCacheFormat, new OpCache())(file)
  }

}

/**
 * Binary formats for cache files.
 */
private[incremental] object OpCacheProtocol extends DefaultProtocol {

  import OpCache.{ FileHash, Record }

  implicit def cacheContentFormat: Format[Map[OpInputHash, Record]] =
    immutableMapFormat[OpInputHash, Record](OpInputHashFormat, RecordFormat)

  implicit def fileDepsFormat: Format[Set[FileHash]] =
    immutableSetFormat[FileHash](FileHashFormat)

  implicit object OpCacheFormat extends Format[OpCache] {
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
    def reads(in: Input): Record = Record(read[Set[FileHash]](in))
    def writes(out: Output, r: Record) = write[Set[FileHash]](out, r.fileHashes)
  }

  implicit object FileHashFormat extends Format[FileHash] {
    def reads(in: Input): FileHash = FileHash(read[File](in), read[Option[Bytes]](in))
    def writes(out: Output, fh: FileHash) = {
      write[File](out, fh.file)
      write[Option[Bytes]](out, fh.sha1IfExists)
    }
  }

}
