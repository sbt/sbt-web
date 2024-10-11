package com.typesafe.sbt.web

import java.nio.file.{ Path => NioPath }
import java.io.{ File => IoFile }
import sbt.*
import sbt.Keys.Classpath
import xsbti.{ FileConverter, HashedVirtualFileRef, VirtualFile }

private[web] object PluginCompat:
  type FileRef = HashedVirtualFileRef
  type Out = VirtualFile

  def toNioPath(a: Attributed[HashedVirtualFileRef])(using conv: FileConverter): NioPath =
    conv.toPath(a.data)
  inline def toFile(a: Attributed[HashedVirtualFileRef])(using conv: FileConverter): File =
    toNioPath(a).toFile()
  def toNioPaths(cp: Seq[Attributed[HashedVirtualFileRef]])(using conv: FileConverter): Vector[NioPath] =
    cp.map(toNioPath).toVector
  inline def toFiles(cp: Seq[Attributed[HashedVirtualFileRef]])(using conv: FileConverter): Vector[File] =
    toNioPaths(cp).map(_.toFile())
  def toSet[A](iterable: Iterable[A]): Set[A] = iterable.to(Set)
  def classpathToFiles(classpath: Classpath): Seq[File] = toFiles(classpath)
end PluginCompat
