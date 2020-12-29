package zio.interop

import zio.random.Random
import zio.test.Assertion._
import zio.test._
import zio.test.Gen
import scalaz.{
  Applicative,
  Apply,
  Bifunctor,
  Bind,
  BindRec,
  Equal,
  Functor,
  InvariantFunctor,
  IsEmpty,
  Monad,
  MonadError,
  MonadPlus,
  Monoid,
  Plus,
  PlusEmpty,
  Semigroup
}
import zio.ZIO

/**
 * Properties that should hold for instances of type classes defined in Scalaz Core.
 */
object ScalazProperties {

  type GenR[A] = Gen[Random, A]

  def checkLaw[A](law: (A) => Boolean)(implicit rv1: GenR[A]): ZIO[Random, Nothing, TestResult] =
    check(rv1)(v1 => assert(law(v1))(isTrue))

  def checkLaw[A, B](
    law: (A, B) => Boolean
  )(implicit rv1: GenR[A], rv2: GenR[B]): ZIO[Random, Nothing, TestResult] =
    check(rv1, rv2)((v1, v2) => assert(law(v1, v2))(isTrue))

  def checkLaw[A, B, C](
    law: (A, B, C) => Boolean
  )(implicit rv1: GenR[A], rv2: GenR[B], rv3: GenR[C]): ZIO[Random, Nothing, TestResult] =
    check(rv1, rv2, rv3)((v1, v2, v3) => assert(law(v1, v2, v3))(isTrue))

  def checkLaw[A, B, C, D](law: (A, B, C, D) => Boolean)(implicit
    rv1: GenR[A],
    rv2: GenR[B],
    rv3: GenR[C],
    rv4: GenR[D]
  ): ZIO[Random, Nothing, TestResult] =
    check(rv1, rv2, rv3, rv4)((v1, v2, v3, v4) => assert(law(v1, v2, v3, v4))(isTrue))

  def checkLaw[A, B, C, D, E](law: (A, B, C, D, E) => Boolean)(implicit
    rv1: GenR[A],
    rv2: GenR[B],
    rv3: GenR[C],
    rv4: GenR[D],
    rv5: GenR[E]
  ): ZIO[Random, Nothing, TestResult] =
    check(rv1, rv2, rv3, rv4, rv5)((v1, v2, v3, v4, v5) => assert(law(v1, v2, v3, v4, v5))(isTrue))

  object semigroup {
    def associative[A](implicit A: Semigroup[A], gen: GenR[A], eq1: Equal[A]) =
      check(gen, gen, gen)((a, b, c) => assert(A.semigroupLaw.associative(a, b, c))(isTrue))

    def laws[A](implicit A: Semigroup[A], gen: GenR[A], eq: Equal[A]) =
      suite("semigroup")(
        testM("associative")(associative[A])
      )
  }

  object monoid {
    def leftIdentity[A](implicit A: Monoid[A], eqa: Equal[A], arb: GenR[A]) =
      checkLaw(A.monoidLaw.leftIdentity _)

    def rightIdentity[A](implicit A: Monoid[A], eqa: Equal[A], arb: GenR[A]) =
      checkLaw(A.monoidLaw.rightIdentity _)

    def laws[A](implicit A: Monoid[A], eqa: Equal[A], arb: GenR[A]) =
      suite("monoid")(
        semigroup.laws[A],
        testM("left identity")(leftIdentity[A]),
        testM("right identity")(rightIdentity[A])
      )
  }

  object invariantFunctor {
    def identity[F[_], X](implicit F: InvariantFunctor[F], afx: GenR[F[X]], ef: Equal[F[X]]) =
      check(afx)(afx => assert(F.invariantFunctorLaw.invariantIdentity[X](afx))(isTrue))

    def composite[F[_], X, Y, Z](implicit
      F: InvariantFunctor[F],
      af: GenR[F[X]],
      axy: GenR[(X => Y)],
      ayz: GenR[(Y => Z)],
      ayx: GenR[(Y => X)],
      azy: GenR[(Z => Y)],
      ef: Equal[F[Z]]
    ) =
      check(af, axy, ayx, ayz, azy)((af, axy, ayx, ayz, azy) =>
        assert(F.invariantFunctorLaw.invariantComposite[X, Y, Z](af, axy, ayx, ayz, azy))(isTrue)
      )

    def laws[F[_]](implicit
      F: InvariantFunctor[F],
      af: GenR[F[Int]],
      axy: GenR[(Int => Int)],
      ef: Equal[F[Int]]
    ) =
      suite("invariantFunctor")(
        testM("identity")(identity[F, Int]),
        testM("composite")(composite[F, Int, Int, Int])
      )
  }

  object functor {
    def identity[F[_], X](implicit F: Functor[F], afx: GenR[F[X]], ef: Equal[F[X]]) =
      checkLaw(F.functorLaw.identity[X] _)

    def composite[F[_], X, Y, Z](implicit
      F: Functor[F],
      af: GenR[F[X]],
      axy: GenR[(X => Y)],
      ayz: GenR[(Y => Z)],
      ef: Equal[F[Z]]
    ) =
      checkLaw(F.functorLaw.composite[X, Y, Z] _)

    def laws[F[_]](implicit F: Functor[F], af: GenR[F[Int]], axy: GenR[(Int => Int)], ef: Equal[F[Int]]) =
      suite("functor")(
        invariantFunctor.laws[F],
        testM("identity")(identity[F, Int]),
        testM("composite")(composite[F, Int, Int, Int])
      )
  }

  object apply { self =>
    def composition[F[_], X, Y, Z](implicit
      ap: Apply[F],
      afx: GenR[F[X]],
      au: GenR[F[Y => Z]],
      av: GenR[F[X => Y]],
      e: Equal[F[Z]]
    ) =
      checkLaw(ap.applyLaw.composition[X, Y, Z] _)

    def laws[F[_]](implicit
      F: Apply[F],
      af: GenR[F[Int]],
      aff: GenR[F[Int => Int]],
      axy: GenR[(Int => Int)],
      e: Equal[F[Int]]
    ) =
      suite("apply")(
        functor.laws[F],
        testM("composition")(self.composition[F, Int, Int, Int])
      )
  }

  object applicative {
    def identity[F[_], X](implicit f: Applicative[F], afx: GenR[F[X]], ef: Equal[F[X]]) =
      checkLaw(f.applicativeLaw.identityAp[X] _)

    def homomorphism[F[_], X, Y](implicit ap: Applicative[F], ax: GenR[X], af: GenR[X => Y], e: Equal[F[Y]]) =
      checkLaw(ap.applicativeLaw.homomorphism[X, Y] _)

    def interchange[F[_], X, Y](implicit
      ap: Applicative[F],
      ax: GenR[X],
      afx: GenR[F[X => Y]],
      e: Equal[F[Y]]
    ) =
      checkLaw(ap.applicativeLaw.interchange[X, Y] _)

    def mapApConsistency[F[_], X, Y](implicit
      ap: Applicative[F],
      ax: GenR[F[X]],
      afx: GenR[X => Y],
      e: Equal[F[Y]]
    ) =
      checkLaw(ap.applicativeLaw.mapLikeDerived[X, Y] _)

    def laws[F[_]](implicit
      F: Applicative[F],
      af: GenR[F[Int]],
      ag: GenR[F[Int => Int]],
      ah: GenR[Int => Int],
      v: GenR[Int],
      e: Equal[F[Int]]
    ) =
      suite("applicative")(
        apply.laws[F],
        testM("identity")(applicative.identity[F, Int]),
        testM("homomorphism")(applicative.homomorphism[F, Int, Int]),
        testM("interchange")(applicative.interchange[F, Int, Int]),
        testM("map consistent with ap")(applicative.mapApConsistency[F, Int, Int])
      )
  }

  object bind {
    def associativity[M[_], X, Y, Z](implicit
      M: Bind[M],
      amx: GenR[M[X]],
      af: GenR[(X => M[Y])],
      ag: GenR[(Y => M[Z])],
      emz: Equal[M[Z]]
    ) =
      checkLaw(M.bindLaw.associativeBind[X, Y, Z] _)

    def bindApConsistency[M[_], X, Y](implicit
      M: Bind[M],
      amx: GenR[M[X]],
      af: GenR[M[X => Y]],
      emy: Equal[M[Y]]
    ) =
      checkLaw(M.bindLaw.apLikeDerived[X, Y] _)

    def laws[M[_]](implicit
      a: Bind[M],
      am: GenR[M[Int]],
      af: GenR[Int => M[Int]],
      ag: GenR[M[Int => Int]],
      ah: GenR[Int => Int],
      e: Equal[M[Int]]
    ) =
      suite("bind")(
        apply.laws[M],
        testM("associativity")(associativity[M, Int, Int, Int]),
        testM("ap consistent with bind")(bindApConsistency[M, Int, Int])
      )
  }

  object bindRec {
    def tailrecBindConsistency[M[_], X](implicit
      M: BindRec[M],
      ax: GenR[X],
      af: GenR[X => M[X]],
      emx: Equal[M[X]]
    ) =
      checkLaw(M.bindRecLaw.tailrecBindConsistency[X] _)

    def laws[M[_]](implicit
      a: BindRec[M],
      am: GenR[M[Int]],
      av: GenR[Int],
      af: GenR[Int => M[Int]],
      ag: GenR[M[Int => Int]],
      ah: GenR[Int => Int],
      e: Equal[M[Int]]
    ) =
      suite("bindRec")(
        bind.laws[M],
        testM("tailrecM is consistent with bind")(bindRec.tailrecBindConsistency[M, Int])
      )
  }

  object monad {
    def rightIdentity[M[_], X](implicit M: Monad[M], e: Equal[M[X]], a: GenR[M[X]]) =
      checkLaw(M.monadLaw.rightIdentity[X] _)

    def leftIdentity[M[_], X, Y](implicit
      am: Monad[M],
      emy: Equal[M[Y]],
      ax: GenR[X],
      af: GenR[(X => M[Y])]
    ) =
      checkLaw(am.monadLaw.leftIdentity[X, Y] _)

    def laws[M[_]](implicit
      a: Monad[M],
      am: GenR[M[Int]],
      af: GenR[Int => M[Int]],
      ag: GenR[M[Int => Int]],
      ah: GenR[Int => Int],
      v: GenR[Int],
      e: Equal[M[Int]]
    ) =
      suite("monad")(
        applicative.laws[M],
        bind.laws[M],
        testM("right identity")(monad.rightIdentity[M, Int]),
        testM("left identity")(monad.leftIdentity[M, Int, Int])
      )
  }

  object plus {
    def associative[F[_], X](implicit f: Plus[F], afx: GenR[F[X]], ef: Equal[F[X]]) =
      checkLaw(f.plusLaw.associative[X] _)

    def laws[F[_]](implicit F: Plus[F], afx: GenR[F[Int]], ef: Equal[F[Int]]) =
      suite("plus")(
        semigroup.laws[F[Int]](F.semigroup[Int], implicitly, implicitly),
        testM("associative")(associative[F, Int])
      )
  }

  object plusEmpty {
    def leftPlusIdentity[F[_], X](implicit f: PlusEmpty[F], afx: GenR[F[X]], ef: Equal[F[X]]) =
      checkLaw(f.plusEmptyLaw.leftPlusIdentity[X] _)

    def rightPlusIdentity[F[_], X](implicit f: PlusEmpty[F], afx: GenR[F[X]], ef: Equal[F[X]]) =
      checkLaw(f.plusEmptyLaw.rightPlusIdentity[X] _)

    def laws[F[_]](implicit F: PlusEmpty[F], afx: GenR[F[Int]], ef: Equal[F[Int]]) =
      suite("plusEmpty")(
        plus.laws[F],
        monoid.laws[F[Int]](F.monoid[Int], implicitly, implicitly),
        testM("left plus identity")(leftPlusIdentity[F, Int]),
        testM("right plus identity")(rightPlusIdentity[F, Int])
      )
  }

  object isEmpty {
    def emptyIsEmpty[F[_], X](implicit f: IsEmpty[F]) =
      assert(f.isEmptyLaw.emptyIsEmpty[X])(isTrue)

    def emptyPlusIdentity[F[_], X](implicit f: IsEmpty[F], afx: GenR[F[X]]) =
      checkLaw(f.isEmptyLaw.emptyPlusIdentity[X] _)

    def laws[F[_]](implicit F: IsEmpty[F], afx: GenR[F[Int]], ef: Equal[F[Int]]) =
      suite("isEmpty")(
        plusEmpty.laws[F],
        test("empty is empty")(emptyIsEmpty[F, Int]),
        testM("empty plus identity")(emptyPlusIdentity[F, Int])
      )
  }

  object monadPlus {
    def emptyMap[F[_], X](implicit f: MonadPlus[F], afx: GenR[X => X], ef: Equal[F[X]]) =
      checkLaw(f.monadPlusLaw.emptyMap[X] _)

    def leftZero[F[_], X](implicit F: MonadPlus[F], afx: GenR[X => F[X]], ef: Equal[F[X]]) =
      checkLaw(F.monadPlusLaw.leftZero[X] _)

    def rightZero[F[_], X](implicit F: MonadPlus[F], afx: GenR[F[X]], ef: Equal[F[X]]) =
      checkLaw(F.strongMonadPlusLaw.rightZero[X] _)

    def laws[F[_]](name: String)(implicit
      F: MonadPlus[F],
      af: GenR[F[Int]],
      ag: GenR[F[Int => Int]],
      ah: GenR[Int => F[Int]],
      ai: GenR[Int => Int],
      v: GenR[Int],
      ef: Equal[F[Int]]
    ) =
      suite(name)(
        monad.laws[F],
        plusEmpty.laws[F],
        testM("empty map")(emptyMap[F, Int]),
        testM("left zero")(leftZero[F, Int])
      )
  }

  object bifunctor {
    def laws[F[_, _]](implicit
      F: Bifunctor[F],
      E: Equal[F[Int, Int]],
      af: GenR[F[Int, Int]],
      axy: GenR[(Int => Int)]
    ) =
      suite("bifunctor")(
        functor.laws[F[?, Int]](F.leftFunctor[Int], implicitly, implicitly, implicitly),
        functor.laws[F[Int, ?]](F.rightFunctor[Int], implicitly, implicitly, implicitly)
      )
  }

  object monadError {
    def raisedErrorsHandled[F[_], E, A](implicit
      me: MonadError[F, E],
      eq: Equal[F[A]],
      ae: GenR[E],
      afea: GenR[E => F[A]]
    )                                                                                                      =
      checkLaw(me.monadErrorLaw.raisedErrorsHandled[A] _)
    def errorsRaised[F[_], E, A](implicit me: MonadError[F, E], eq: Equal[F[A]], ae: GenR[E], aa: GenR[A]) =
      checkLaw(me.monadErrorLaw.errorsRaised[A] _)
    def errorsStopComputation[F[_], E, A](implicit
      me: MonadError[F, E],
      eq: Equal[F[A]],
      ae: GenR[E],
      aa: GenR[A]
    )                                                                                                      =
      checkLaw(me.monadErrorLaw.errorsStopComputation[A] _)

    def laws[F[_], E](implicit
      me: MonadError[F, E],
      f: GenR[F[Int]],
      ag: GenR[F[Int => Int]],
      ah: GenR[E => F[Int]],
      ai: GenR[Int => F[Int]],
      aj: GenR[Int => Int],
      ve: GenR[E],
      v: GenR[Int],
      e: Equal[F[Int]]
    ) =
      suite("monad error")(
        monad.laws[F],
        testM("raisedErrorsHandled")(raisedErrorsHandled[F, E, Int]),
        testM("errorsRaised")(errorsRaised[F, E, Int]),
        testM("errorsStopComputation")(errorsStopComputation[F, E, Int])
      )
  }
}
