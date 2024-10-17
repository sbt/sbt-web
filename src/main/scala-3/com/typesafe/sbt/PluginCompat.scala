package com.typesafe.sbt

import java.nio.file.{ Path => NioPath }
import java.io.{ File => IoFile }
import sbt.*
import sbt.Keys.Classpath
import xsbti.{ FileConverter, HashedVirtualFileRef, VirtualFile }

private[sbt] object PluginCompat:
  type FileRef = HashedVirtualFileRef
  type Out = VirtualFile

  def toNioPath(a: Attributed[HashedVirtualFileRef])(using conv: FileConverter): NioPath =
    conv.toPath(a.data)
  inline def toFile(a: Attributed[HashedVirtualFileRef])(using conv: FileConverter): File =
    toNioPath(a).toFile
  def toNioPaths(cp: Seq[Attributed[HashedVirtualFileRef]])(using conv: FileConverter): Vector[NioPath] =
    cp.map(toNioPath).toVector
  inline def toFiles(cp: Seq[Attributed[HashedVirtualFileRef]])(using conv: FileConverter): Vector[File] =
    toNioPaths(cp).map(_.toFile)
  def toSet[A](iterable: Iterable[A]): Set[A] = iterable.to(Set)
  inline def classpathToFiles(classpath: Classpath)(using conv: FileConverter): Seq[File] =
    toFiles(classpath.to(Seq))
  inline def toKey(settingKey: SettingKey[String]): StringAttributeKey = StringAttributeKey(settingKey.key.label)
  def toNioPath(hvf: HashedVirtualFileRef)(using conv: FileConverter): NioPath =
    conv.toPath(hvf)
  def toFile(hvf: HashedVirtualFileRef)(using conv: FileConverter): File =
    toNioPath(hvf).toFile
  inline def toFileRef(file: File)(using conv: FileConverter): FileRef =
    conv.toVirtualFile(file.toPath)
  inline def selectFirstPredicate(using conv: FileConverter): Seq[FileRef] => Boolean = files =>
    files.forall(toFile(_).isFile) && files.map(_.contentHashStr).distinct.size == 1
end PluginCompat
