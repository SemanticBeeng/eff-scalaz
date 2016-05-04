package org.atnos.eff

import Eff._
import scalaz._
import scalaz.syntax.applicative._

sealed trait Choose[T]
case class ChooseZero[T]() extends Choose[T]
case object ChoosePlus extends Choose[Boolean]

/**
 * The Choose effect models non-determinism
 * So we can get results, either:
 *   - no results (when using ChooseZero)
 *   - the result for action1 or the result for action b (when using ChoosePlus)
 *
 * When running this effect we can "collect" the results with any
 * F which has an Alternative instance.
 *
 * For example if F is List then:
 *  - no results is the empty list
 *  - the result for a or b is List(a, b)
 *
 * If F is Option then:
 *  - no results is the None
 *  - the result for a or b is Some(a) or Some(b
 */
trait ChooseEffect extends
  ChooseCreation with
  ChooseInterpretation

object ChooseEffect extends ChooseEffect

trait ChooseCreation {
  def zero[R, A](implicit m: Choose <= R): Eff[R, A] =
    send[Choose, R, A](ChooseZero[A]())

  def plus[R, A](a1: Eff[R, A], a2: =>Eff[R, A])(implicit m: Choose <= R): Eff[R, A] =
    EffMonad[R].bind(send(ChoosePlus))((b: Boolean) => if (b) a1 else a2)

  def chooseFrom[R, A](as: List[A])(implicit m: Choose <= R): Eff[R, A] =
    as match {
      case Nil => send[Choose, R, A](ChooseZero[A]())
      case a :: rest => plus(EffMonad[R].point(a), chooseFrom(rest))
    }
}

object ChooseCreation extends ChooseCreation

trait ChooseInterpretation {
  def runChoose[R <: Effects, U <: Effects, A, F[_] : Alternative](r: Eff[R, A])(implicit m: Member.Aux[Choose, R, U]): Eff[U, F[A]] = {
    r match {
      case Pure(a) =>
        EffMonad[U].point(ApplicativePlus[F].point(a))

      case Impure(u, c) =>
        m.project(u) match {
          case -\/(u1) =>
            Impure(u1, Arrs.singleton((x: u1.X) => runChoose(c(x))))

          case \/-(choose) =>
            choose match {
              case ChooseZero() => EffMonad[U].point(ApplicativePlus[F].empty)
              case _ =>
                val continuation = c.asInstanceOf[Arrs[R, Boolean, A]]
                (runChoose(continuation(true)) |@| runChoose(continuation(false)))((a, b) => ApplicativePlus[F].plus(a, b))
            }
        }
    }
  }
}

object ChooseInterpretation extends ChooseInterpretation

trait ChooseImplicits {
  /**
   * MonadPlus implementation for the Eff[R, ?] type if R contains the Choose effect
   */
  def EffMonadPlus[R](implicit m: Member[Choose, R]): MonadPlus[Eff[R, ?]] = new MonadPlus[Eff[R, ?]] {
    def point[A](a: =>A): Eff[R, A] =
      EffMonad[R].point(a)

    def bind[A, B](fa: Eff[R, A])(f: A => Eff[R, B]): Eff[R, B] =
      EffMonad[R].bind(fa)(f)

    def empty[A]: Eff[R, A] =
      ChooseEffect.zero[R, A]

    def plus[A](a1: Eff[R, A], a2: =>Eff[R, A]): Eff[R, A] =
      ChooseEffect.plus(a1, a2)
  }

}

object ChooseImplicits extends ChooseImplicits
