package org.specs2.example

import org.specs2.Specification
import org.specs2.control.eff._
import scalaz._, Scalaz._
import ReaderEffect._
import WriterEffect._
import EvalEffect._
import Effects._
import Member._
import Eff._

class ReadmeSpec extends Specification { def is = s2"""

 run the first example $firstExample
 future effect $futureEffect

"""

  def firstExample = {

    object StackEffects {
      type Stack = ReaderInt |: WriterString |: Eval |: NoEffect

      /**
       * Those declarations are necessary to guide implicit resolution
       * but they only need to be done once per stack
       */
      type ReaderInt[X] = Reader[Int, X]
      type WriterString[X] = Writer[String, X]

      implicit def ReaderMember: Member[ReaderInt, Stack] =
        Member.MemberNatIsMember

      implicit def WriterMember: Member[WriterString, Stack] =
        Member.MemberNatIsMember

      implicit def EvalMember: Member[Eval, Stack] =
        Member.MemberNatIsMember
    }

    import StackEffects._

    // create an action
    val action: Eff[Stack, Int] = for {
    // get the configuration
      init <- ask[Stack, Int]

      // log the current configuration value
      _ <- tell[Stack, String]("START: the start value is "+init)

      // compute the nth power of 2
      a <- delay(powerOfTwo(init))

      // log an end message
      _ <- tell[Stack, String]("END")
    } yield a

    // run the action with all the interpreters
    val result: (Int, List[String]) =
      action |> runReader(5) |> runWriter |> runEval |> run

    result === ((32, List("START: the start value is 5", "END")))
  }

  import scala.concurrent._, duration._
  import scala.concurrent.ExecutionContext.Implicits.global
  import Interpret._

  def futureEffect = {
    object FutureEffect {

      type Fut[A] = Future[() => A]

      def future[R, A](a: =>A)(implicit m: Fut <= R): Eff[R, A] =
        send[Fut, R, A](Future(() => a))

      def runFuture[R <: Effects, A, B](atMost: Duration)(effects: Eff[Fut |: R, A]): Eff[R, A] = {
        val recurse = new Recurse[Fut, R, A] {
          def apply[X](m: Fut[X]): X \/ Eff[R, A] =
            -\/(Await.result(m.map(_()), atMost))
        }
        interpret1((a: A) => a)(recurse)(effects)
      }
    }

    import FutureEffect._

    type F = Fut |: NoEffect
    implicit def FutMember: Fut <= F =
      Member.MemberNatIsMember

    val action: Eff[F, Int] = for {
      a <- future(1)
      b <- future(2)
    } yield a + b

    (action |> runFuture(3.seconds) |> run) ==== 3
  }

  def powerOfTwo(n: Int): Int =
    math.pow(2, n.toDouble).toInt
}
