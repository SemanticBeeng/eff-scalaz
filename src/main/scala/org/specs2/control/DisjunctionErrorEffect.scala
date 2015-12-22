package org.specs2.control

import Eff._
import Effects._
import Member._

import scala.util.control.NonFatal
import scalaz._, Scalaz._

/**
 * Effect for computation which can fail and return a Throwable, or just stop with a failure
 */
object DisjunctionErrorEffect {

  type Error = Throwable \/ String

  type DisjunctionError[A] = Error \/ Name[A]

  def ok[R, A](a: =>A)(implicit m: DisjunctionError <= R): Eff[R, A] =
    try   impure(m.inject(\/-(Name(a))), Arrs.singleton((a: A) => EffMonad[R].point(a)))
    catch { case t: Throwable => exception(t) }

  def error[R, A](error: Error)(implicit m: DisjunctionError <= R): Eff[R, A] =
    impure(m.inject(-\/(error)), Arrs.singleton((a: A) => EffMonad[R].point(a)))

  def fail[R, A](message: String)(implicit m: DisjunctionError <= R): Eff[R, A] =
    error(\/-(message))

  def exception[R, A](t: Throwable)(implicit m: DisjunctionError <= R): Eff[R, A] =
    error(-\/(t))

  def runDisjunctionError[R <: Effects, A](r: Eff[DisjunctionError |: R, A]): Eff[R, Error \/ A] = {
    val recurse = new Recurse[DisjunctionError, R, Error \/ A] {
      def apply[X](m: DisjunctionError[X]) =
        m match {
          case -\/(e) =>
            \/-(EffMonad[R].point(-\/(e)))

          case \/-(a) =>
            try -\/(a.value)
            catch { case NonFatal(t) => \/-(EffMonad[R].point(-\/(-\/(t)))) }
        }
    }

    interpret1[R, DisjunctionError, A, Error \/ A]((a: A) => \/-(a))(recurse)(r)
  }

  implicit class DisjunctionErrorEffectOps[R, A](action: Eff[R, A]) {
    def andFinally(last: Eff[R, Unit])(implicit m: DisjunctionError <= R): Eff[R, A] =
      DisjunctionErrorEffect.andFinally(action, last)

    def orElse(action2: Eff[R, A])(implicit m: DisjunctionError <= R): Eff[R, A] =
      DisjunctionErrorEffect.orElse(action, action2)
  }

  /**
   * evaluate 2 actions possibly having disjunction error effects
   *
   * The second action must be executed whether the first is successful or not
   */
  def andFinally[R, A](action: Eff[R, A], last: Eff[R, Unit])(implicit m: DisjunctionError <= R): Eff[R, A] =
    (action, last) match {

      case (Pure(e), Pure(l)) =>
        action

      case (Pure(_), Impure(u, c)) =>
        action >>= ((a: A) => last.as(a))

      case (Impure(u1, c1), Impure(u2, c2)) =>
        (m.project(u1), m.project(u2)) match {
          case (Some(\/-(e1)), Some(\/-(e2))) =>
            ok {
              try     c1(e1.value).andFinally(last)
              catch { case NonFatal(t) => e2.value; DisjunctionErrorEffect.exception[R, A](t)(m) }
            }(m).flatMap(identity _)

          case (None, Some(\/-(e2))) =>
            last.flatMap(_ => action)

          case _ =>
            action
        }

      case _ =>
        action
    }

  /**
   * evaluate 2 actions possibly having disjunction error effects
   *
   * The second action must be executed if the first one is not successful
   */
  def orElse[R, A](action1: Eff[R, A], action2: Eff[R, A])(implicit m: DisjunctionError <= R): Eff[R, A] =
    (action1, action2) match {
      case (Pure(_), _) =>
        action1

      case (Impure(u1, c1), _) =>
        m.project(u1) match {
          case Some(\/-(e1)) =>
            try c1(e1.value.asInstanceOf[A]).orElse(action2)
            catch { case NonFatal(_) => action2 }

          case Some(-\/(_)) =>
            action2

          case None =>
            action1
        }
    }

  implicit class DisjunctionErrorOps[A](c: Error \/ A) {
    def toErrorSimpleMessage: Option[String] =
      c match {
        case -\/(e) => Some(e.simpleMessage)
        case _      => None
      }

    def toErrorFullMessage: Option[String] =
      c match {
        case -\/(e) => Some(e.fullMessage)
        case _      => None
      }
  }

  implicit class ErrorOps[A](e: Error) {
    def simpleMessage: String =
      e match {
        case -\/(t) => render(t)
        case \/-(m) => m
      }

    def fullMessage: String =
      e match {
        case -\/(t) => renderWithStack(t)
        case \/-(m) => m
      }
  }


  def render(t: Throwable): String =
    s"Error[${t.getClass.getName}]" + (Option(t.getMessage) match {
      case None          => ""
      case Some(message) => s" ${message}"
    })

  def renderWithStack(t: Throwable): String =
    s"""============================================================
       |${render(t)}
       |------------------------------------------------------------
       |${traceWithIndent(t, "    ")}
       |============================================================
       |""".stripMargin

  def trace(t: Throwable): String =  {
    val out = new java.io.StringWriter
    t.printStackTrace(new java.io.PrintWriter(out))
    out.toString
  }

  def traceWithIndent(t: Throwable, indent: String): String =
    trace(t).lines.map(line => indent + line).mkString("\n")

}

