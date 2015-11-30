package org.specs2.control

import scalaz.{Coproduct=>_,Inject=>_,:+: => _,_}, Scalaz._
import Eff._
import Effects._
import Members._
import shapeless._, shapeless.ops.coproduct._

sealed trait Writer[O, X]

case class Put[O](o: () => O) extends Writer[O, Unit]

object Writer {

  def put[O](o: => O): Writer[O, Unit] =
    Put(() => o)

  def tell[R <: Coproduct, O](o: => O)(implicit member: Members[Writer[O, ?], R], inject: Inject[R, Writer[O, Unit]]): Eff[R, Unit] =
    // the type annotation is necessary here to prevent a compiler error
    send[Writer[O, ?], R, Unit](put(o))


  /**
   * runWriter :: Eff (Writer o ’: r ) a → Eff r (a,[ o])
   * runWriter =
   * handle relay (\x → return (x,[]))
   * (\(Put o) k → k () = \(x,l ) → return (x, o: l ))
   */

  def runWriter[R <: Coproduct, O, A](w: Eff[Writer[O, A] :+: R, A]): Eff[R, (A, Vector[O])] = {
    val putOne = (a: A) => EffMonad[R].point((a, Vector[O]()))

    val putRest = new EffCont[Writer[O, ?], R, (A, Vector[O])] {
      def apply[X] = (w: Writer[O, X]) => (continuation: X => Eff[R, (A, Vector[O])]) => w match {
        case Put(o) => continuation(()) >>= ((xl: (A, Vector[O])) => EffMonad.point((xl._1, xl._2 :+ o)))
      }
    }

    relay[R, Writer[O, ?], A, (A, Vector[O])](putOne, putRest)(w)
  }



  type WriterStack[O, E <: Effects] = Writer[O, ?] <:: E

  implicit def WriterMember[O, E <: Effects]: Member[Writer[O, ?], WriterStack[O, E]] =
    Member.EffectMember[Writer[O, ?], E]


}
