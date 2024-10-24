import sbt.*

object FileAssertions {
  def assertExists(file: File): Unit =
    assert(file.exists(), s"Could not find ${file.getName}")
}
