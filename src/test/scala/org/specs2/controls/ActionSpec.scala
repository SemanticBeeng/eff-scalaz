package org.specs2
package controls

import Action._
import control.{Eval, Checked, Writer, Eff, Member, Effects, MemberNat}
import Eval._ 
import Checked.runChecked  
import Writer._    
import Eff._  
import scalaz.{Writer => _, Reader => _,_}, Scalaz._, effect.IO

class ActionSpec extends Specification with ScalaCheck { def is = s2"""

 The action stack can be used to 
   compute values                      $computeValues
   stop when there is an error         $stop
   display log messages                $logMessages            
   collect warnings                    $collectWarnings
 
"""

  def computeValues =
    runWith(2, 3).map(_._1) must beRight(5)

  def stop =
    runWith(20, 30) must_== Left("too big")

  def logMessages = {
    val messages = new scala.collection.mutable.ListBuffer[String]  
    runWith(1, 2, m => messages.append(m))
    
    messages.toList === List("got the value 1", "got the value 2")
  }

  def collectWarnings =
    runWith(2, 3).map(_._2) must beRight(Vector("the sum is big: 5"))   
    

  /**
   * HELPERS
   */
  
  def runWith(i: Int, j: Int, printer: String => Unit = s => ()): Either[String, (Int, Vector[String])] =
    run(runEval(runChecked(runWarnings(runConsoleToPrinter(printer)(actions(i, j))))))

  /**
   * ActionStack actions: no annotation is necessary here
   */
  def actions(i: Int, j: Int): Eff[ActionStack, Int] = for {
    x <- evalIO(IO(i))
    _ <- log("got the value "+x)
    y <- evalIO(IO(j))
    _ <- log("got the value "+y)
    s <- if (x + y > 10) Checked.ko("too big") else Checked.ok(x + y)
    _ <- if (s >= 5) warn("the sum is big: "+s) else Eff.unit[ActionStack]        
  } yield s

  /**
   * "open" effects version of the same actions
   * this one can be reused with more effects
   */
  def unboundActions[R](i: Int, j: Int)(
    implicit m1: Member[Eval, R],
             m2: Member[Console, R],
             m3: Member[Warnings, R],
             m4: Member[CheckedString, R]
  ): Eff[R, Int] = for {
    x <- evalIO[R, Int](IO(i))
    _ <- log[R]("got the value "+x)
    y <- evalIO[R, Int](IO(j))
    _ <- log[R]("got the value "+y)
    s <- if (x + y > 10) Checked.ko[R, String, Int]("too big") else Checked.ok[R, String, Int](x + y)
    _ <- if (s >= 5) warn[R]("the sum is big: "+s) else Eff.unit[R]        
  } yield s

}
