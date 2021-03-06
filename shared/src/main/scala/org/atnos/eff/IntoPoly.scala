package org.atnos.eff

import scalaz._
import Eff._

/**
 * Typeclass proving that it is possible to send a tree of effects R into another tree of effects U
 *
 * for example
 *
 *  send[Option1, Fx.fx3[Option1, Option2, Option3], Int](Option1(1)).
 *    into[Fx.fx5[Option1, Option2, Option3, Option4, Option5]]
 *
 *  should work because all the effects of the first stack are present in the second
 *
 * Note: some implicit definitions are probably missing in some cases
 */
trait IntoPoly[R, U] {
  def apply[A](e: Eff[R, A]): Eff[U, A]
}

object IntoPoly extends IntoPolyLower1

trait IntoPolyLower1 extends IntoPolyLower2 {

  implicit def intoNil[R]: IntoPoly[NoFx, R] =
    new IntoPoly[NoFx, R] {
      def apply[A](e: Eff[NoFx, A]) =
        e match { case Pure(a) => pure[R, A](a); case _ => sys.error("impossible NoFx into R is only for pure values") }
    }

  implicit def intoSelf[R]: IntoPoly[R, R] =
    new IntoPoly[R, R] { def apply[A](e: Eff[R, A]) = e }

}

trait IntoPolyLower2  extends IntoPolyLower3 {

  implicit def intoAppendL2L[T1[_], T2[_], R]: IntoPoly[FxAppend[Fx1[T2], R], FxAppend[Fx2[T1, T2], R]] =
    new IntoPoly[FxAppend[Fx1[T2], R], FxAppend[Fx2[T1, T2], R]] {
      def apply[A](e: Eff[FxAppend[Fx1[T2], R], A]): Eff[FxAppend[Fx2[T1, T2], R], A] =
        e match {
          case Pure(a) =>
            EffMonad[FxAppend[Fx2[T1, T2], R]].point(a)

          case Impure(u@UnionAppendR(r), c) =>
            Impure[FxAppend[Fx2[T1, T2], R], u.X, A](UnionAppendR(r), Arrs.singleton(x => effInto(c(x))))

          case Impure(u@UnionAppendL(Union1(tx)), c) =>
            Impure[FxAppend[Fx2[T1, T2], R], u.X, A](UnionAppendL(Union2R(tx)), Arrs.singleton(x => effInto(c(x))))

          case ImpureAp(u@UnionAppendR(r), c) =>
            ImpureAp[FxAppend[Fx2[T1, T2], R], u.X, A](UnionAppendR(r), Apps(c.functions.map(f => effInto[FxAppend[Fx1[T2], R], FxAppend[Fx2[T1, T2], R], Any => Any](f))))

          case ImpureAp(u@UnionAppendL(Union1(tx)), c) =>
            ImpureAp[FxAppend[Fx2[T1, T2], R], u.X, A](UnionAppendL(Union2R(tx)), Apps(c.functions.map(f => effInto[FxAppend[Fx1[T2], R], FxAppend[Fx2[T1, T2], R], Any => Any](f))))
        }
    }

  implicit def intoAppendL2R[T1[_], T2[_], R]: IntoPoly[FxAppend[Fx1[T1], R], FxAppend[Fx2[T1, T2], R]] =
    new IntoPoly[FxAppend[Fx1[T1], R], FxAppend[Fx2[T1, T2], R]] {
      def apply[A](e: Eff[FxAppend[Fx1[T1], R], A]): Eff[FxAppend[Fx2[T1, T2], R], A] =
        e match {
          case Pure(a) =>
            EffMonad[FxAppend[Fx2[T1, T2], R]].point(a)

          case Impure(u@UnionAppendR(r), c) =>
            Impure[FxAppend[Fx2[T1, T2], R], u.X, A](UnionAppendR(r), Arrs.singleton(x => effInto(c(x))))

          case Impure(u@UnionAppendL(Union1(tx)), c) =>
            Impure[FxAppend[Fx2[T1, T2], R], u.X, A](UnionAppendL(Union2L(tx)), Arrs.singleton(x => effInto(c(x))))

          case ImpureAp(u@UnionAppendR(r), c) =>
            ImpureAp[FxAppend[Fx2[T1, T2], R], u.X, A](UnionAppendR(r), Apps(c.functions.map(f => effInto[FxAppend[Fx1[T1], R], FxAppend[Fx2[T1, T2], R], Any => Any](f))))

          case ImpureAp(u@UnionAppendL(Union1(tx)), c) =>
            ImpureAp[FxAppend[Fx2[T1, T2], R], u.X, A](UnionAppendL(Union2L(tx)), Apps(c.functions.map(f => effInto[FxAppend[Fx1[T1], R], FxAppend[Fx2[T1, T2], R], Any => Any](f))))
        }
    }

  implicit def intoAppendL3L[T1[_], T2[_], T3[_], R]: IntoPoly[FxAppend[Fx2[T2, T3], R], FxAppend[Fx3[T1, T2, T3], R]] =
    new IntoPoly[FxAppend[Fx2[T2, T3], R], FxAppend[Fx3[T1, T2, T3], R]] {
      def apply[A](e: Eff[FxAppend[Fx2[T2, T3], R], A]): Eff[FxAppend[Fx3[T1, T2, T3], R], A] =
        e match {
          case Pure(a) =>
            EffMonad[FxAppend[Fx3[T1, T2, T3], R]].point(a)

          case Impure(u@UnionAppendR(r), c) =>
            Impure[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendR(r), Arrs.singleton(x => effInto(c(x))))

          case Impure(u@UnionAppendL(Union2L(tx)), c) =>
            Impure[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendL(Union3M(tx)), Arrs.singleton(x => effInto(c(x))))

          case Impure(u@UnionAppendL(Union2R(tx)), c) =>
            Impure[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendL(Union3R(tx)), Arrs.singleton(x => effInto(c(x))))

          case ImpureAp(u@UnionAppendR(r), c) =>
            ImpureAp[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendR(r), Apps(c.functions.map(f => effInto[FxAppend[Fx2[T2, T3], R], FxAppend[Fx3[T1, T2, T3], R], Any => Any](f))))

          case ImpureAp(u@UnionAppendL(Union2L(tx)), c) =>
            ImpureAp[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendL(Union3M(tx)), Apps(c.functions.map(f => effInto[FxAppend[Fx2[T2, T3], R], FxAppend[Fx3[T1, T2, T3], R], Any => Any](f))))

          case ImpureAp(u@UnionAppendL(Union2R(tx)), c) =>
            ImpureAp[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendL(Union3R(tx)), Apps(c.functions.map(f => effInto[FxAppend[Fx2[T2, T3], R], FxAppend[Fx3[T1, T2, T3], R], Any => Any](f))))
        }
    }

  implicit def intoAppendL3M[T1[_], T2[_], T3[_], R]: IntoPoly[FxAppend[Fx2[T1, T3], R], FxAppend[Fx3[T1, T2, T3], R]] =
    new IntoPoly[FxAppend[Fx2[T1, T3], R], FxAppend[Fx3[T1, T2, T3], R]] {
      def apply[A](e: Eff[FxAppend[Fx2[T1, T3], R], A]): Eff[FxAppend[Fx3[T1, T2, T3], R], A] =
        e match {
          case Pure(a) =>
            EffMonad[FxAppend[Fx3[T1, T2, T3], R]].point(a)

          case Impure(u@UnionAppendR(r), c) =>
            Impure[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendR(r), Arrs.singleton(x => effInto(c(x))))

          case Impure(u@UnionAppendL(Union2L(tx)), c) =>
            Impure[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendL(Union3L(tx)), Arrs.singleton(x => effInto(c(x))))

          case Impure(u@UnionAppendL(Union2R(tx)), c) =>
            Impure[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendL(Union3R(tx)), Arrs.singleton(x => effInto(c(x))))

          case ImpureAp(u@UnionAppendR(r), c) =>
            ImpureAp[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendR(r), Apps(c.functions.map(f => effInto[FxAppend[Fx2[T1, T3], R], FxAppend[Fx3[T1, T2, T3], R], Any => Any](f))))

          case ImpureAp(u@UnionAppendL(Union2L(tx)), c) =>
            ImpureAp[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendL(Union3L(tx)), Apps(c.functions.map(f => effInto[FxAppend[Fx2[T1, T3], R], FxAppend[Fx3[T1, T2, T3], R], Any => Any](f))))

          case ImpureAp(u@UnionAppendL(Union2R(tx)), c) =>
            ImpureAp[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendL(Union3R(tx)), Apps(c.functions.map(f => effInto[FxAppend[Fx2[T1, T3], R], FxAppend[Fx3[T1, T2, T3], R], Any => Any](f))))
        }
    }

  implicit def intoAppendL3R[T1[_], T2[_], T3[_], R]: IntoPoly[FxAppend[Fx2[T1, T2], R], FxAppend[Fx3[T1, T2, T3], R]] =
    new IntoPoly[FxAppend[Fx2[T1, T2], R], FxAppend[Fx3[T1, T2, T3], R]] {
      def apply[A](e: Eff[FxAppend[Fx2[T1, T2], R], A]): Eff[FxAppend[Fx3[T1, T2, T3], R], A] =
        e match {
          case Pure(a) =>
            EffMonad[FxAppend[Fx3[T1, T2, T3], R]].point(a)

          case Impure(u@UnionAppendR(r), c) =>
            Impure[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendR(r), Arrs.singleton(x => effInto(c(x))))

          case Impure(u@UnionAppendL(Union2L(tx)), c) =>
            Impure[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendL(Union3L(tx)), Arrs.singleton(x => effInto(c(x))))

          case Impure(u@UnionAppendL(Union2R(tx)), c) =>
            Impure[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendL(Union3M(tx)), Arrs.singleton(x => effInto(c(x))))

          case ImpureAp(u@UnionAppendR(r), c) =>
            ImpureAp[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendR(r), Apps(c.functions.map(f => effInto[FxAppend[Fx2[T1, T2], R], FxAppend[Fx3[T1, T2, T3], R], Any => Any](f))))

          case ImpureAp(u@UnionAppendL(Union2L(tx)), c) =>
            ImpureAp[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendL(Union3L(tx)), Apps(c.functions.map(f => effInto[FxAppend[Fx2[T1, T2], R], FxAppend[Fx3[T1, T2, T3], R], Any => Any](f))))

          case ImpureAp(u@UnionAppendL(Union2R(tx)), c) =>
            ImpureAp[FxAppend[Fx3[T1, T2, T3], R], u.X, A](UnionAppendL(Union3M(tx)), Apps(c.functions.map(f => effInto[FxAppend[Fx2[T1, T2], R], FxAppend[Fx3[T1, T2, T3], R], Any => Any](f))))
        }
    }
}

trait IntoPolyLower3 extends IntoPolyLower4 {
  implicit def intoAppendL1[T[_], R]: IntoPoly[R, FxAppend[Fx1[T], R]] =
    new IntoPoly[R, FxAppend[Fx1[T], R]] {
      def apply[A](e: Eff[R, A]): Eff[FxAppend[Fx1[T], R], A] =
        e match {
          case Pure(a) =>
            EffMonad[FxAppend[Fx1[T], R]].point(a)

          case Impure(u, c) =>
            Impure[FxAppend[Fx1[T], R], u.X, A](UnionAppendR(u), Arrs.singleton(x => effInto(c(x))))

          case ImpureAp(u, c) =>
            ImpureAp[FxAppend[Fx1[T], R], u.X, A](UnionAppendR(u), Apps(c.functions.map(f => effInto[R, FxAppend[Fx1[T], R], Any => Any](f))))
        }
    }
}

trait IntoPolyLower4 {

  implicit def into[T[_], R, Q, U, S](implicit
                                      t: Member.Aux[T, R, S],
                                      m: T |= U,
                                      recurse: IntoPoly[S, U]): IntoPoly[R, U] =
    new IntoPoly[R, U] {
      def apply[A](e: Eff[R, A]): Eff[U, A] =
        e match {
          case Pure(a) =>
            EffMonad[U].point(a)

          case Impure(u, c) =>
            t.project(u) match {
              case \/-(tx) => Impure[U, u.X, A](m.inject(tx), Arrs.singleton(x => effInto(c(x))))
              case -\/(s)   => recurse(Impure[S, s.X, A](s, c.asInstanceOf[Arrs[S, s.X, A]]))
            }

          case ImpureAp(u, c) =>
            t.project(u) match {
              case \/-(tx) => ImpureAp[U, u.X, A](m.inject(tx), Apps(c.functions.map(f => effInto[R, U, Any => Any](f))))
              case -\/(s)   => recurse(ImpureAp[S, u.X, A](s, c.asInstanceOf[Apps[S, s.X, A]]))
            }
        }
    }
}

