import sbt.*

object FileAssertions {
  def assertLibrary(
      target: File,
      id: String,
      location: LibraryLocation = Library,
      altName: Option[String] = None
  ): Unit = {
    val filename = altName.getOrElse(id)
    val fileToVerify = location match {
      case Root =>
        target / "web" / "public" / "main" / "js" / s"$filename.js"
      case Library =>
        target / "web" / "public" / "main" / "lib" / id / "js" / s"$filename.js"
      case TestRoot =>
        target / "web" / "public" / "test" / "js" / s"$filename.js"
      case TestLibrary =>
        target / "web" / "public" / "test" / "lib" / id / "js" / s"$filename.js"
      case External(path) =>
        target / "web" / "public" / path / "lib" / id / s"$filename.js"
    }
    //println(fileToVerify.toString)
    assert(fileToVerify.exists(), s"Could not find $filename.js")
  }
}

sealed trait LibraryLocation
case object Root extends LibraryLocation
case object Library extends LibraryLocation
case object TestRoot extends LibraryLocation
case object TestLibrary extends LibraryLocation
case class External(path: String = "main") extends LibraryLocation
