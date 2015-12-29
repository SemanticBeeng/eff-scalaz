package org.specs2.control.eff

import com.ambiata.disorder._
import org.specs2.{ScalaCheck, Specification}
import Eff._
import Effects._
import ReaderEffect._
import OptionEffect._

import scalaz._, Scalaz._

class OptionEffectSpec extends Specification with ScalaCheck { def is = s2"""

 run the option monad                     $optionMonad
 run the option monad with nothing        $optionWithNothingMonad
 run the option monad with reader         $optionReader

 The Eff monad is stack safe with Option  $stacksafeOption

"""

  def optionMonad = {
    type S = Option |: NoEffect

    val option: Eff[S, String] =
      for {
        s1 <- OptionEffect.some[S, String]("hello")
        s2 <- OptionEffect.some[S, String]("world")
      } yield s1 + " " + s2

    run(runOption(option)) === Some("hello world")
  }

  def optionWithNothingMonad = {
    type S = Option |: NoEffect

    val option: Eff[S, String] =
      for {
        s1 <- OptionEffect.some[S, String]("hello")
        s2 <- OptionEffect.none[S, String]
      } yield s1 + " " + s2

    run(runOption(option)) === None
  }

  def optionReader = prop { (init: PositiveIntSmall, someValue: PositiveIntSmall) =>

    // define a Reader / Option stack
    type R[A] = Reader[Int, A]
    type S = Option |: R |: NoEffect
    import MemberNat._

    implicit def ReaderStackMember: Member[R, S] =
      Member.MemberNatIsMember

    // create actions
    val readOption: Eff[S, Int] =
      for {
        j <- OptionEffect.some[S, Int](someValue.value)
        i <- ask[S, Int]
      } yield i + j

    // run effects
    val initial = init.value

    run(runReader(initial)(runOption(readOption))) must_==
      Some(initial + someValue.value)
  }

  def stacksafeOption = {
    type E = Option |: NoEffect
    implicit def OptionMember: Member[Option, E] =
      Member.MemberNatIsMember

    val list = (1 to 5000).toList
    val action = list.traverseU(i => OptionEffect.some(i))

    run(OptionEffect.runOption(action)) ==== Some(list)
  }

}
