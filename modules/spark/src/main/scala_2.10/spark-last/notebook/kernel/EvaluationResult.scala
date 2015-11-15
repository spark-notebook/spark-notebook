package notebook.kernel

import xml.NodeSeq

/**
 * Result of evaluating something in the REPL.
 *
 * The difference between Incomplete and Failure is Incomplete means
 * the expression failed to compile whereas Failure means an exception
 * was thrown during executing the code.
 */
sealed abstract class EvaluationResult

case object Incomplete extends EvaluationResult
case class Failure(stackTrace: String) extends EvaluationResult
case class Success(result: NodeSeq) extends EvaluationResult
