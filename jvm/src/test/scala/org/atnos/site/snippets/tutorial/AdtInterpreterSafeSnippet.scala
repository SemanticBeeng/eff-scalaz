// 8<---
package org.atnos.site.snippets.tutorial

import AdtSnippet._
import AdtCreationSnippet._
import scalaz._

trait AdtInterpreterSafeSnippet {
// 8<---

import org.atnos.eff._, all._, interpret._, syntax._
import scalaz._

type _writerString[R] = Writer[String, ?] |= R
type _stateMap[R]     = State[Map[String, Any], ?] |= R

/**
 * Safe interpreter for KVStore effects
 *
 * It uses the following effects:
 *
 *  - Writer to create log statements
 *  - State to update a key-value Map
 *  - \/ to raise errors if the type of an object in the map is not of the expected type
 *
 *  The resulting effect stack is U which is R without the KVStore effects
 *
 *  Note that we just require the Throwable, Writer and State effects to
 *  be able to be created in the stack U
 *
 * This interpreter uses the org.atnos.eff.interpreter.translate method
 * translating one effect of the stack to other effects in the same stack
 *
 *
 * NOTE: It is really important for type inference that the effects for U are listed after those for R!
 *
 * Implicit member definitions will NOT be found with the following definition:
 *
 * def runKVStore[R, U :_throwableOr :_writerString :_stateMap, A](effects: Eff[R, A]) (
 *   implicit m: Member.Aux[KVStore, R, U]): Eff[U, A] = {
 *
 */
def runKVStore[R, U, A](effects: Eff[R, A])
  (implicit m: Member.Aux[KVStore, R, U],
            throwable:_throwableOr[U],
            writer:_writerString[U],
            state:_stateMap[U]): Eff[U, A] = {

  translate(effects)(new Translate[KVStore, U] {
    def apply[X](kv: KVStore[X]): Eff[U, X] =
      kv match {
        case Put(key, value) =>
          for {
            _ <- tell(s"put($key, $value)")
            _ <- modify((map: Map[String, Any]) => map.updated(key, value))
            r <- fromDisjunction(\/.fromTryCatchNonFatal(().asInstanceOf[X]))
          } yield r

        case Get(key) =>
          for {
            _ <- tell(s"get($key)")
            m <- get[U, Map[String, Any]]
            r <- fromDisjunction(\/.fromTryCatchNonFatal(m.get(key).asInstanceOf[X]))
          } yield r

        case Delete(key) =>
          for {
            _ <- tell(s"delete($key)")
            u <- modify((map: Map[String, Any]) => map - key)
            r <- fromDisjunction(\/.fromTryCatchNonFatal(().asInstanceOf[X]))
          } yield r
      }
  })
}

// 8<---
}

object AdtInterpreterSafeSnippet extends AdtInterpreterSafeSnippet
