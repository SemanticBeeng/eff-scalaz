package org.specs2.control.eff

import org.specs2.Specification
import Eff._
import Effects._
import EvalEffect._

import scalaz._, Scalaz._

class EvalEffectSpec extends Specification { def is = s2"""

 run is stack safe with Eval   $stacksafeRun
 run is stack safe with handleRelay   $stacksafeHandleRelay

"""

  def stacksafeRun = {
    type E = Eval |: NoEffect

    val list = (1 to 5000).toList
    val action = list.traverseU(i => EvalEffect.delay[E, Int](i))

    run(runEval(action)) ==== list
  }

  def stacksafeHandleRelay = {
    type E = Eval |: NoEffect

    val list = (1 to 5000).toList
    val action = list.traverseU(i => EvalEffect.delay[E, Int](i))

    run(runEvalRelay(action)) ==== list
  }

  def stacksafeAttempt = {
    type E = Eval |: NoEffect

    val list = (1 to 5000).toList
    val action = list.traverseU(i => EvalEffect.delay[E, Int](i))

    run(attemptEval(action)) ==== \/-(list)
  }
}
