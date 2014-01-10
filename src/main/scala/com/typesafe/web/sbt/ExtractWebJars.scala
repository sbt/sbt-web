package com.typesafe.web.sbt

import scala.collection.JavaConversions._
import java.net.{URI, URL}
import java.io.File
import java.util.jar.JarFile
import org.webjars.{WebJarExtractor, FileSystemCache}

object ExtractWebJars {

  // FIXME: Some of this behaviour should go back into the WebJar extractor code itself.

  /** Discover all the WebJars on the given classloader */
  def discoverWebJars(classLoader: ClassLoader): Seq[String] = {
    def findWebJarInURL(resource: URL): Seq[String] = {
      resource.getProtocol match {
        case "jar" =>
          val urlPath = resource.getPath
          val file = new File(URI.create(urlPath.substring(0, urlPath.indexOf("!"))))
          val jarFile = new JarFile(file)
          try {
            jarFile.entries().toStream
              .filter(_.isDirectory)
              .map(e => new File(e.getName))
              .collect {
                case webjar if "META-INF/resources/webjars" == webjar.getParent => webjar.getName
              }
              .toList
          } finally {
            jarFile.close()
          }
        case "file" =>
          val file = new File(resource.toURI)
          if (file.isDirectory) {
            file.listFiles().toSeq.collect {
              case dir if dir.isDirectory => dir.getName
            }
          } else Nil
        case _ => Nil
      }
    }

    (for {
      webJarResource <- classLoader.getResources("META-INF/resources/webjars").toSeq
      webJar <- findWebJarInURL(webJarResource)
    } yield webJar).toList
  }

  /**
   * Filter the given webjars by the given exclude/include filter
   *
   * The filter sequences are either exact matches, or `*` to match anything.
   */
  def filterWebJars(webJars: Seq[String], includes: Seq[String], excludes: Seq[String]): Seq[String] = {
    def matches(candidate: String, filter: String) = filter == "*" || filter == candidate
    def filter(candidate: String, filters: Seq[String]) = filters.exists(matches(candidate, _))

    webJars
      .filter(filter(_, includes))
      .filterNot(filter(_, excludes))
  }

  /** Extract WebJars to the given folder */
  def extractWebJars(classLoader: ClassLoader, webJars: Seq[String], dest: File, cacheFile: File): File = {
    val cache = new FileSystemCache(cacheFile)
    val extractor = new WebJarExtractor(cache, classLoader)
    webJars.foreach { webJar =>
      extractor.extractWebJarTo(webJar, dest)
    }
    cache.save()
    dest
  }

}
