package org.atnos.eff.syntax

import scalaz._
import org.atnos.eff._
import EvalEffect._

object eval extends eval

trait eval {

  implicit class EvalEffectOps[R <: Effects, A](e: Eff[R, A]) {

    def runEval(implicit member: Member[Eval, R]): Eff[member.Out, A] =
      EvalInterpretation.runEval(e)(member.aux)

    def attemptEval(implicit member: Member[Eval, R]): Eff[member.Out, Throwable \/ A] =
      EvalInterpretation.attemptEval(e)(member.aux)

  }

}

