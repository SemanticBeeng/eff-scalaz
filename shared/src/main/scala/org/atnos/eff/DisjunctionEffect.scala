package org.atnos.eff

import scalaz._
import Eff._
import Interpret._

/**
 * Effect for computation which can fail
 */
trait DisjunctionEffect extends
  DisjunctionCreation with
  DisjunctionInterpretation

object DisjunctionEffect extends DisjunctionEffect

trait DisjunctionCreation {

  /** create a Disjunction effect from a single Option value */
  def optionDisjunction[R, E, A](option: Option[A], e: E)(implicit member: (E \/ ?) <= R): Eff[R, A] =
    option.fold[Eff[R, A]](left[R, E, A](e))(right[R, E, A])

  /** create a  Disjunction effect from a single \/ value */
  def fromDisjunction[R, E, A](disjunction: E \/ A)(implicit member: (E \/ ?) <= R): Eff[R, A] =
    disjunction.fold[Eff[R, A]](left[R, E, A], right[R, E, A])

  /** create a failed value */
  def left[R, E, A](e: E)(implicit member: (E \/ ?) <= R): Eff[R, A] =
    send[E \/ ?, R, A](-\/(e))

  /** create a correct value */
  def right[R, E, A](a: A)(implicit member: (E \/ ?) <= R): Eff[R, A] =
    send[E \/ ?, R, A](\/-(a))

}

object DisjunctionCreation extends DisjunctionCreation

trait DisjunctionInterpretation {

  /** run the disjunction effect, yielding E \/A */
  def runDisjunction[R, U, E, A](r: Eff[R, A])(implicit m: Member.Aux[(E \/ ?), R, U]): Eff[U, E \/ A] = {
    val recurse = new Recurse[(E \/ ?), U, E \/ A] {
      def apply[X](m: E \/ X) =
        m match {
          case -\/(e) => \/-(EffMonad[U].point(\/.left(e)))
          case \/-(a) => -\/(a)
        }
    }

    interpret1[R, U, (E \/ ?), A, E \/ A]((a: A) => \/.right(a): E \/ A)(recurse)(r)
  }

  /** run the disjunction effect, yielding Either[E, A] */
  def runEither[R, U, E, A](r: Eff[R, A])(implicit m: Member.Aux[(E \/ ?), R, U]): Eff[U, Either[E, A]] =
    runDisjunction(r).map(_.fold(util.Left.apply, util.Right.apply))

  /** catch and handle a possible left value */
  def catchLeft[R, E, A](r: Eff[R, A])(handle: E => Eff[R, A])(implicit member: (E \/ ?) <= R): Eff[R, A] = {
    val recurse = new Recurse[(E \/ ?), R, A] {
      def apply[X](m: E \/ X) =
        m match {
          case -\/(e) => \/-(handle(e))
          case \/-(a) => -\/(a)
        }
    }

    intercept1[R, (E \/ ?), A, A]((a: A) => a)(recurse)(r)
  }


  /**
   * Lift a computation over a "small" error (for a subsystem) into
   * a computation over a "bigger" error (for the full application)
   */
  def localDisjunction[SR, BR, U, E1, E2, A](r: Eff[SR, A], getter: E1 => E2)
                                            (implicit sr: Member.Aux[E1 \/ ?, SR, U],
                                                      br: Member.Aux[E2 \/ ?, BR, U]): Eff[BR, A] =
  transform[SR, BR, U, E1 \/ ?, E2 \/ ?, A](r,
    new ~>[E1 \/ ?, E2 \/ ?] {
      def apply[X](r: E1 \/ X): E2 \/ X =
        r.leftMap(getter)
    })

}

object DisjunctionInterpretation extends DisjunctionInterpretation
