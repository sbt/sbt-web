package com.typesafe.sbt

import sbt.*
import sbt.Keys.Classpath
import xsbti.FileConverter

import java.nio.file.Path as NioPath

private[sbt] object PluginCompat {
  type FileRef = java.io.File
  type Out = java.io.File

  def toNioPath(a: Attributed[File])(implicit conv: FileConverter): NioPath =
    a.data.toPath
  def toFile(a: Attributed[File])(implicit conv: FileConverter): File =
    a.data
  def toNioPaths(cp: Seq[Attributed[File]])(implicit conv: FileConverter): Vector[NioPath] =
    cp.map(_.data.toPath()).toVector
  def toFiles(cp: Seq[Attributed[File]])(implicit conv: FileConverter): Vector[File] =
    cp.map(_.data).toVector
  def toSet[A](iterable: Iterable[A]): Set[A] = iterable.to[Set]
  def classpathToFiles(classpath: Classpath)(implicit conv: FileConverter): Seq[FileRef] =
    classpath.files
  def toKey( settingKey: SettingKey[String] ): AttributeKey[String] = settingKey.key
  def toNioPath(f: File)(implicit conv: FileConverter): NioPath =
    f.toPath
  def toFile(f: File)(implicit conv: FileConverter): File = f
}
