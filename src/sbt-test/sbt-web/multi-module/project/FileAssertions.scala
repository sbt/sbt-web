import sbt.*

object FileAssertions {
  def assertLibrary(
      target: File,
      id: String,
      location: LibraryLocation = Library(),
      altName: Option[String] = None
  ): Unit = {
    val filename = altName.getOrElse(id)
    val fileToVerify = location match {
      case Root(stage) =>
        target / "web" / "public" / stage / "js" / s"$filename.js"
      case Library(stage) =>
        target / "web" / "public" / stage / "lib" / id / "js" / s"$filename.js"
      case External(path) =>
        target / "web" / "public" / path / "lib" / id / s"$filename.js"
    }
    assert(fileToVerify.exists(), s"Could not find $filename.js")
  }
}

sealed trait LibraryLocation
case class Root(stage: String = "main") extends LibraryLocation
case class Library(stage: String = "main") extends LibraryLocation
case class External(stage: String = "main") extends LibraryLocation
