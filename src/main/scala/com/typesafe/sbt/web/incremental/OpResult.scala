/*
 * Copyright (C) 2009-2013 Typesafe Inc. <http://www.typesafe.com>
 */
package com.typesafe.sbt.web.incremental

import java.io.File
import scala.collection.immutable.Set
import xsbti.Problem

/**
 * The result of running an operation, either OpSuccess or OpFailure.
 */
sealed trait OpResult

/**
 * An operation that succeeded. Contains information about which files the
 * operation read and wrote.
 */
final case class OpSuccess(filesRead: Set[File], filesWritten: Set[File]) extends OpResult

/**
 * An operation that failed.
 */
final case object OpFailure extends OpResult