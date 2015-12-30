package org.specs2.benchmarks

import org.scalameter.api._
import org.specs2.control.eff._
import EvalEffect._
import Effects._
import Eff._
import scalaz._, Scalaz._
import org.scalameter.picklers.Implicits._

object EffBenchmark extends Bench.OfflineReport {
  type E = Eval |: NoEffect

  val sizes = Gen.enumeration("size")(10, 100, 1000, 10000, 100000)

  val lists = for {
    size <- sizes
  } yield (0 until size).toList

  def simpleSend[R, V](v: =>V)(implicit m: Member[Eval, R]) =
    impure(m.inject(Name(v)), Arrs.singleton((v: V) => EffMonad[R].point(v)))


  performance of "send" in {
    measure method "simple send" in {
      using(lists) in { list =>
        list.traverseU(a => simpleSend[E, Int](a)) |> runEval |> run
      }
    }
    measure method "optimised send" in {
      using(lists) in { list =>
        list.traverseU(a => delay[E, Int](a)) |> runEval |> run
      }
    }
  }

}
