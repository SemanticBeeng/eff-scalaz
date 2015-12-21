package org.specs2.control

import Eff._
import Effects._
import Member._
import scalaz._

/**
 * Effect for logging values alongside computations
 */
object WriterEffect {

  def write[O](o: O): Writer[O, Unit] =
    Writer(o, ())

  def tell[R, O](o: O)(implicit member: Member[Writer[O, ?], R]): Eff[R, Unit] =
    // the type annotation is necessary here to prevent a compiler error
    send[Writer[O, ?], R, Unit](write(o))

  def runWriter[R <: Effects, O, A](w: Eff[Writer[O, ?] <:: R, A]): Eff[R, (A, List[O])] = {
    val recurse: StateRecurse[Writer[O, ?], A, (A, List[O])] = new StateRecurse[Writer[O, ?], A, (A, List[O])] {
      type S = List[O]
      val init = List[O]()
      def apply[X](x: Writer[O, X], l: List[O]) = (x.run._2, l :+ x.run._1)
      def finalize(a: A, l: List[O]) = (a, l)
    }

    interpretState1[R, Writer[O, ?], A, (A, List[O])]((a: A) => (a, List[O]()))(recurse)(w)
  }
}
