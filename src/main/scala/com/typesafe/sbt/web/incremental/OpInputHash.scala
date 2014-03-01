/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.sbt.web.incremental

import sbt.Hash

/**
 * The result of hashing the input
 */
sealed trait OpInputHashResult

/**
 * Used to indicate that this operation should not be cached
 */
case object NoCache extends OpInputHashResult

/**
 * A hash of an operation's input. Used to check if two operations have the
 * same or different inputs.
 */
case class OpInputHash(bytes: Bytes) extends OpInputHashResult

/**
 * Factory methods for OpInputHash.
 */
object OpInputHash {
  /**
   * Hash the given bytes.
   */
  def hashBytes(bytes: Array[Byte]): OpInputHash = new OpInputHash(Bytes(Hash(bytes)))
  /**
   * Hash the given string.
   */
  def hashString(s: String, encoding: String = "UTF-8"): OpInputHash = hashBytes(s.getBytes(encoding))
}

/**
 * Given an operation, produces a hash of its inputs.
 */
trait OpInputHasher[Op] {
  def hash(op: Op): OpInputHashResult
}

/**
 * Factory methods for OpInputHash.
 */
object OpInputHasher {
  /**
   * Construct an OpInputHash that uses the given hashing logic.
   */
  def apply[Op](f: Op => OpInputHashResult): OpInputHasher[Op] = new OpInputHasher[Op] {
    def hash(op: Op) = f(op)
  }

  def noCache[Op]: OpInputHasher[Op] = new OpInputHasher[Op] {
    def hash(op: Op) = NoCache
  }
}

