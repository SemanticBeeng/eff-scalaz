package org.atnos.eff

import Eff._
import scala.collection.mutable.ListBuffer
import scalaz._
import \/._
import Interpret._

/**
 * Effect for computations possibly returning several values
 */
trait ListEffect extends
  ListCreation with
  ListInterpretation

object ListEffect extends ListEffect

trait ListCreation {

  type _List[R] = List <= R
  type _list[R] = List |= R

  /** create a list effect with no values */
  def empty[R :_list, A]: Eff[R, A] =
    fromList(List())

  /** create a list effect from a single value */
  def singleton[R :_list, A](a: A): Eff[R, A] =
    fromList(List(a))

  /** create a list effect from a list of values */
  def values[R :_list, A](as: A*): Eff[R, A] =
    fromList(as.toList)

  /** create a list effect from a list of values */
  def fromList[R :_list, A](as: List[A]): Eff[R, A] =
    send[List, R, A](as)
}

object ListCreation extends ListCreation

trait ListInterpretation {
  /** run an effect stack starting with a list effect */
  def runList[R, U, A](effects: Eff[R, A])(implicit m: Member.Aux[List, R, U]): Eff[U, List[A]] = {
    val loop = new Loop[List, R, A, Eff[U, List[A]]] {
      type S = (List[Eff[R, A]], ListBuffer[A])
      val init = (List[Eff[R, A]](), new ListBuffer[A])

      def onPure(a: A, s: S): (Eff[R, A], S) \/ Eff[U, List[A]] =
        s match {
          case (head :: tail, result) => -\/((head, (tail, result :+ a)))
          case (List(), result)       => \/-(EffMonad[U].point((result :+ a).toList))
        }

      def onEffect[X](l: List[X], continuation: Arrs[R, X, A], s: S): (Eff[R, A], S) \/ Eff[U, List[A]] =
        (l, s) match {
          case (List(), (head :: tail, result)) =>
            -\/((head, (tail, result)))

          case (List(), (List(), result)) =>
            \/-(EffMonad[U].point(result.toList))

          case (head :: tail, (unevaluated, result)) =>
            -\/((continuation(head), (tail.map(a => continuation(a)) ++ unevaluated, result)))
        }
    }

    interpretLoop1((a: A) => List(a))(loop)(effects)
  }
}

object ListInterpretation extends ListInterpretation
