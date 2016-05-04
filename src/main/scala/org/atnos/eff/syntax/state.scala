package org.atnos.eff.syntax

import scalaz._
import org.atnos.eff._

object state extends state

trait state {

  implicit class StateEffectOps[R <: Effects, A](e: Eff[R, A]) {

    def runState[S, U <: Effects](s: S)(implicit member: Member.Aux[State[S,  ?], R, U]): Eff[U, (A, S)] =
      StateInterpretation.runState(s)(e)

    def runStateTagged[S, U <: Effects, T](s: S)(implicit member: Member.Aux[({type l[X] = State[S, X] @@ T})#l, R, U]): Eff[U, (A, S)] =
      StateInterpretation.runStateTagged(s)(e)

    def runStateZero[S : Monoid, U <: Effects](implicit member: Member.Aux[State[S,  ?], R, U]): Eff[U, (A, S)] =
      StateInterpretation.runStateZero(e)

    def evalState[S, U <: Effects](s: S)(implicit member: Member.Aux[State[S,  ?], R, U]): Eff[U, A] =
      StateInterpretation.evalState(s)(e)

    def evalStateTagged[S, U <: Effects, T](s: S)(implicit member: Member.Aux[({type l[X] = State[S, X] @@ T})#l, R, U]): Eff[U, A] =
      StateInterpretation.evalStateTagged(s)(e)

    def evalStateZero[S : Monoid, U <: Effects](implicit member: Member.Aux[State[S,  ?], R, U]): Eff[U, A] =
      StateInterpretation.evalStateZero(e)

    def execState[S, U <: Effects](s: S)(implicit member: Member.Aux[State[S,  ?], R, U]): Eff[U, S] =
      StateInterpretation.execState(s)(e)

    def execStateZero[S : Monoid, U <: Effects](implicit member: Member.Aux[State[S,  ?], R, U]): Eff[U, S] =
      StateInterpretation.execStateZero(e)

    def execStateTagged[S, U <: Effects, T](s: S)(implicit member: Member.Aux[({type l[X] = State[S, X] @@ T})#l, R, U]): Eff[U, S] =
      StateInterpretation.execStateTagged(s)(e)

    def lensState[BR, U, T, S](getter: S => T, setter: (S, T) => S)(implicit m1: Member.Aux[State[T, ?], R, U], m2: Member.Aux[State[S, ?], BR, U]): Eff[BR, A] =
      StateInterpretation.lensState[R, BR, U, T, S, A](e, getter, setter)

  }

}



