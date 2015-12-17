package org.specs2
package control

import Eff._
import Effects._
import org.specs2.control.Member._
import scala.util.control.NonFatal
import scalaz._, Scalaz._
import scalaz.effect.IO


sealed trait Eval[A] {
  def value: A
}

case class Evaluate[A](v: () => A) extends Eval[A] {
  def value: A =
    v()
}

object Eval {

  def now[R, A](a: A)(implicit m: Member[Eval, R]): Eff[R, A] =
    pure(a)

  def delay[R, A](a: =>A)(implicit m: Member[Eval, R]): Eff[R, A] =
    impure(m.inject(Evaluate[A](() => a)), Arrs.singleton((a: A) => EffMonad[R].point(a)))

  def evalIO[R, A](a: IO[A])(implicit m: Member[Eval, R]): Eff[R, A] =
    delay(a.unsafePerformIO)

  def runEval[R <: Effects, A](r: Eff[Eval[?] <:: R, A]): Eff[R, A] = {
    val runImpure = new EffCont[Eval, R, A] {
      def apply[X](r: Eval[X])(continuation: X => Eff[R, A]): Eff[R, A] =
        continuation(r.value)
    }

    relay1[R, Eval, A, A]((a: A) => a)(runImpure)(r)
  }

  def attemptEval[R <: Effects, A](r: Eff[Eval[?] <:: R, A]): Eff[R, Throwable \/ A] = {
    val runImpure = new EffCont[Eval, R, Throwable \/ A] {
      def apply[X](r: Eval[X])(continuation: X => Eff[R, Throwable \/ A]): Eff[R, Throwable \/ A] =
        try { continuation(r.value) }
        catch { case NonFatal(t) => Eff.pure(-\/(t)) }
    }

    relay1[R, Eval, A, Throwable \/ A]((a: A) => \/-(a))(runImpure)(r)
  }

  implicit class AndFinally[R, A](action: Eff[R, A]) {
    def andFinally(last: Eff[R, Unit])(implicit m: Eval <= R): Eff[R, A] =
      Eval.andFinally(action, last)
  }

  /** evaluate 2 actions possibly having eval effects */
  def andFinally[R, A](action: Eff[R, A], last: Eff[R, Unit])(implicit m: Eval <= R): Eff[R, A] =
    (action, last) match {
      case (_, Pure(l))                     => action
      case (Pure(_), Impure(u, c))          => action >>= ((a: A) => last.as(a))
      case (Impure(u1, c1), Impure(u2, c2)) =>
        (m.project(u1), m.project(u2)) match {
          case (Some(e1), Some(e2)) =>
            Eval.delay { try e1.value.asInstanceOf[A] finally { e2.value; () } }

          case _ => action
        }
    }

}

