package org.specs2.control

import scala.annotation.tailrec
import scalaz._
import Effects._
import Union.decompose

/**
 * Effects of type R, returning a value of type A
 *
 * It is implemented as a "Free-er" monad with extensible effects:
 *
 *  - the "pure" case is a pure value of type A
 *
 *  - the "impure" case is:
 *     - a disjoint union of possible effects
 *     - a continuation of type X => Eff[R, A] indicating what to do if the current effect is of type M[X]
 *       this type is represented by the `Arrs` type
 *
 * The monad implementation for this type is really simple:
 *
 *  - `point` is Pure
 *  - `bind` simply appends the binding function to the `Arrs` continuation
 *
 * Important:
 *
 *  The list of continuations is NOT implemented as a type sequence but simply as a
 *    Vector[Any => Eff[R, Any]]
 *
 *  This means that various `.asInstanceOf` are present in the implementation and could lead
 *  to burns and severe harm. Use with caution!
 *
 * @see http://okmij.org/ftp/Haskell/extensible/more.pdf
 *
 */
sealed trait Eff[R, A]

case class Pure[R, A](value: A) extends Eff[R, A]

/**
 * union is a disjoint union of effects returning a value of type X (not specified)
 */
case class Impure[R, X, A](union: Union[R, X], continuation: Arrs[R, X, A]) extends Eff[R, A]

object Eff {

  /**
   * Monad implementation for the Eff[R, ?] type
   */
  implicit def EffMonad[R]: Monad[Eff[R, ?]] = new Monad[Eff[R, ?]] {
    def point[A](a: => A): Eff[R, A] =
      Pure(a)

    def bind[A, B](fa: Eff[R, A])(f: A => Eff[R, B]): Eff[R, B] =
      fa match {
        case Pure(a) =>
          f(a)

        case Impure(union, continuation) =>
          Impure(union, continuation.append(f))
      }
  }

  /** create an Eff[R, A] value from an effectful value of type T[V] provided that T is one of the effects of R */
  def send[T[_], R, V](tv: T[V])(implicit member: Member[T, R]): Eff[R, V] =
    impure(member.inject(tv), Arrs.singleton((v: V) => EffMonad[R].point(v)))

  /** create an Eff value for () */
  def unit[R]: Eff[R, Unit] =
    EffMonad.point(())

  /** create a pure value */
  def pure[R, A](a: A): Eff[R, A] =
    Pure(a)

  /** create a impure value from an union of effects and a continuation */
  def impure[R, X, A](union: Union[R, X], continuation: Arrs[R, X, A]): Eff[R, A] =
    Impure[R, X, A](union, continuation)

  /**
   * base runner for an Eff value having no effects at all
   *
   * This runner can only return the value in Pure because it doesn't
   * known how to interpret the effects in Impure
   */
  def run[A](eff: Eff[NoEffect, A]): A =
    eff match {
      case Pure(a) => a
      case other   => sys.error("impossible: cannot run the effects in "+other)
    }

  /**
   * Operations of Eff[R, A] values
   */

  implicit class EffOps[R <: Effects, A](e: Eff[R, A]) {
    def into[U](implicit f: IntoPoly[R, U, A]): Eff[U, A] =
      effInto(e)(f)
  }

  /**
   * An Eff[R, A] value can be transformed into an Eff[U, A]
   * value provided that all the effects in R are also in U
   */
  def effInto[R <: Effects, U, A](e: Eff[R, A])(implicit f: IntoPoly[R, U, A]): Eff[U, A] =
    f(e)

  /**
   * Trait for polymorphic recursion into Eff[?, A]
   *
   * The idea is to deal with one effect at the time:
   *
   *  - if the effect stack is M |: R and if U contains M
   *    we transform each "Union[R, X]" in the Impure case into a Union for U
   *    and we try to recurse on other effects present in R
   *
   *  - if the effect stack is M |: NoEffect and if U contains M we
   *    just "inject" the M[X] effect into Eff[U, A] using the Member typeclass
   *    if M is not present when we decompose we throw an exception. This case
   *    should never happen because if there is no other effect in the stack
   *    there should be at least something producing a value of type A
   *
   */
  trait IntoPoly[R <: Effects, U, A] {
    def apply(e: Eff[R, A]): Eff[U, A]
  }

  implicit def intoNoEff[M[_], U, A](implicit m: Member[M, M |: NoEffect], mu: Member[M, U]): IntoPoly[M |: NoEffect, U, A] =
    new IntoPoly[M |: NoEffect, U, A] {
      def apply(e: Eff[M |: NoEffect, A]): Eff[U, A] = {

        e match {
          case Pure(a) =>
            EffMonad[U].point(a)

          case Impure(u, c) =>
            decompose(u) match {
              case \/-(mx) => impure[U, u.X, A](mu.inject(mx), Arrs.singleton(x => effInto(c(x))))
              case -\/(u1) => sys.error("impossible")
            }
        }
      }
    }

  implicit def intoEff[M[_], R <: Effects, U, A](implicit m: Member[M, M |: R], mu: Member[M, U], recurse: IntoPoly[R, U, A]): IntoPoly[M |: R, U, A] =
    new IntoPoly[M |: R, U, A] {
      def apply(e: Eff[M |: R, A]): Eff[U, A] = {

        e match {
          case Pure(a) =>
            EffMonad[U].point(a)

          case Impure(u, c) =>
            decompose(u) match {
              case \/-(mx) => impure[U, u.X, A](mu.inject(mx), Arrs.singleton(x => effInto(c(x))))
              case -\/(u1) => recurse(impure[R, u1.X, A](u1, c.asInstanceOf[Arrs[R, u1.X, A]]))
            }
        }
      }
    }
}


/**
 * Sequence of monadic functions from A to B: A => Eff[B]
 *
 * Internally it is represented as a Vector of functions:
 *
 *  A => Eff[R, X1]; X1 => Eff[R, X2]; X2 => Eff[R, X3]; ...; X3 => Eff[R, B]
 *
 */
case class Arrs[R, A, B](functions: Vector[Any => Eff[R, Any]]) {

  /**
   * append a new monadic function to this list of functions such that
   *
   * Arrs[R, A, B] => (B => Eff[R, C]) => Arrs[R, A, C]
   *
   */
  def append[C](f: B => Eff[R, C]): Arrs[R, A, C] =
    Arrs(functions :+ f.asInstanceOf[Any => Eff[R, Any]])

  /**
   * execute this monadic function
   *
   * This method is stack-safe
   */
  def apply(a: A): Eff[R, B] = {
    @tailrec
    def go(fs: Vector[Any => Eff[R, Any]], v: Any): Eff[R, B] = {
      fs match {
        case Vector(f) =>
          f(v).asInstanceOf[Eff[R, B]]

        case f +: rest =>
          f(v) match {
            case Pure(a1) => go(rest, a1)
            case Impure(u, q) => Impure[R, u.X, B](u, q.copy(functions = q.functions ++ rest))
          }
      }
    }

    go(functions, a)
  }
}

object Arrs {

  /** create an Arrs function from a single monadic function */
  def singleton[R, A, B](f: A => Eff[R, B]): Arrs[R, A, B] =
    Arrs(Vector(f.asInstanceOf[Any => Eff[R, Any]]))
}
