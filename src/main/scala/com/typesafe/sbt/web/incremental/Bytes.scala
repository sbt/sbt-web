/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.sbt.web.incremental

import java.util

/**
 * Wraps a byte array to ensure immutability.
 */
class Bytes(private[incremental] val arr: Array[Byte]) {
  override def toString: String = util.Arrays.toString(arr)
  override def equals(that: Any): Boolean = that match {
    case null         => false
    case other: Bytes => util.Arrays.equals(arr, other.arr)
    case _            => false
  }
  override def hashCode: Int = util.Arrays.hashCode(arr)
}

/**
 * Wraps a byte array to ensure immutability.
 */
private[incremental] object Bytes {

  /**
   * Create a Bytes object with a clone of the given array.
   */
  def apply(arr: Array[Byte]): Bytes = new Bytes(arr.clone)
}
