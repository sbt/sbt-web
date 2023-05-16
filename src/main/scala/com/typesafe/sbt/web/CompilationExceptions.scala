package com.typesafe.sbt.web

import sbt._
import xsbti._
import java.util.Optional

object CompileProblems {

  type LoggerReporter = sbt.internal.inc.ManagedLoggedReporter

  /**
   * Report compilation problems using the given reporter.
   * 
   * If there are any compilation problems with Error severity, then a
   * `CompileProblemsException` is thrown. The exception will contain
   * the list of problems.
   */
  def report[Op,A](reporter: Reporter, problems: Seq[Problem]): Unit = {
    reporter.reset()
    problems.foreach(reporter.log)
    reporter.printSummary()
    if (problems.exists(_.severity() == Severity.Error)) { throw new CompileProblemsException(problems.toArray) }
  }

}

/**
 * Exception thrown by `CompileProblems.report` if there are any problems
 * to report. This exception contains the problems so they can be used
 * for further processing (e.g. for display by Play).
 */
class CompileProblemsException(override val problems: Array[Problem])
  extends CompileFailed
  with FeedbackProvidedException {

  override val arguments: Array[String] = Array.empty
}

/**
 * Capture a general problem with the compilation of a source file. General problems
 * have no associated line number and are always regarded as errors.
 * @param message The message to report.
 * @param source The source file containing the general error.
 */
class GeneralProblem(val message: String, source: File) extends Problem {
  def category(): String = ""

  def severity(): Severity = Severity.Error

  def position(): Position = new Position {
    def line(): Optional[Integer] = Optional.empty()

    def lineContent(): String = ""

    def offset(): Optional[Integer] = Optional.empty()

    def pointer(): Optional[Integer] = Optional.empty()

    def pointerSpace(): Optional[String] = Optional.empty()

    def sourcePath(): Optional[String] = Optional.of(source.getCanonicalPath)

    def sourceFile(): Optional[File] = Optional.of(source)
  }
}

/**
 * Capture a line/column position along with the line's content for a given source file.
 * @param lineNumber The line number - starts at 1.
 * @param lineContent The content of the line itself.
 * @param characterOffset The offset character position - starts at 0.
 * @param source The associated source file.
 */
class LinePosition(
                    lineNumber: Int,
                    override val lineContent: String,
                    characterOffset: Int,
                    source: File
                    ) extends Position {
  def line(): Optional[Integer] = Optional.of(lineNumber)

  def offset(): Optional[Integer] = Optional.of(characterOffset)

  def pointer(): Optional[Integer] = offset()

  def pointerSpace(): Optional[String] = Optional.of(
    lineContent.take(pointer().get).map {
      case '\t' => '\t'
      case x => ' '
    })

  def sourcePath(): Optional[String] = Optional.of(source.getPath)

  def sourceFile(): Optional[File] = Optional.of(source)

  override def toString = s"$source:$lineNumber:$characterOffset"
}

/**
 * Capture a problem associated with a line number and character offset.
 */
class LineBasedProblem(
                        override val message: String,
                        override val severity: Severity,
                        lineNumber: Int,
                        characterOffset: Int,
                        lineContent: String,
                        source: File
                        ) extends Problem {

  def category(): String = ""

  override def position: Position = new LinePosition(lineNumber, lineContent, characterOffset, source)
}