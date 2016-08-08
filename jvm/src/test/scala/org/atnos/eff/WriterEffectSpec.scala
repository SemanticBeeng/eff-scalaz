package org.atnos.eff

import scalaz._
import Scalaz._
import org.atnos.eff._, all._
import org.atnos.eff.syntax.all._
import org.specs2.Specification

import scala.collection.mutable.ListBuffer

class WriterEffectSpec extends Specification { def is = s2"""

 A writer effect can use a side-effecting fold to be evaluated $sideEffecting

 A writer effect can use an Eval fold to be evaluated $evalWriting

"""

  def sideEffecting = {
    type S = Fx.fx1[Writer[String, ?]]

    val action: Eff[S, String] = for {
      f <- tell[S, String]("hello")
      h <- tell[S, String]("world")
    } yield f+" "+h

    val messages: ListBuffer[String] = new ListBuffer[String]

    action.runWriterUnsafe((m: String) => messages.append(m))

    messages.toList ==== List("hello", "world")

  }

  def evalWriting = {
    type S = Fx.fx2[Writer[String, ?], Eval]

    val action: Eff[S, String] = for {
      f <- tell[S, String]("hello")
      h <- tell[S, String]("world")
    } yield f+" "+h

    val messages: ListBuffer[String] = new ListBuffer[String]

    action.runWriterEval((m: String) => Name(messages.append(m))).runEval

    messages.toList ==== List("hello", "world")

  }

}

