package zio
package interop

import org.scalacheck.{ Arbitrary, Cogen, _ }
import org.specs2.{ ScalaCheck, Specification }
import scalaz.Scalaz._
import scalaz.scalacheck.ScalazProperties._
import scalaz._
import zio.interop.scalaz72._
import zio.internal.PlatformLive

class scalazPlatform72Spec extends Specification with ScalaCheck with GenIO {
  def is = s2"""
    laws must hold for
      Bifunctor              ${bifunctor.laws[IO]}
      BindRec                ${bindRec.laws[IO[Int, ?]]}
      Plus                   ${plus.laws[IO[Int, ?]]}
      MonadPlus              ${monadPlus.laws[IO[Int, ?]]}
      MonadPlus (Monoid)     ${monadPlus.laws[IO[Option[Unit], ?]]}
      MonadError             ${monadError.laws[IO[Int, ?], Int]}
      Applicative (Parallel) ${applicative.laws[scalaz72.ParIO[Any, Int, ?]]}
  """
  type Env = Any

  private val rts: Runtime[Env] = new DefaultRuntime {
    override val Platform = PlatformLive
      .makeDefault()
      .withReportFailure(_ => ())
  }

  implicit def ioEqual[E: Equal, A: Equal]: Equal[IO[E, A]] =
    new Equal[IO[E, A]] {
      override def equal(io1: IO[E, A], io2: IO[E, A]): Boolean =
        rts.unsafeRun(io1.either) === rts.unsafeRun(io2.either)
    }

  implicit def ioArbitrary[E: Arbitrary: Cogen, A: Arbitrary: Cogen]: Arbitrary[IO[E, A]] =
    Arbitrary(genIO[E, A])

  implicit def ioParEqual[E: Equal, A: Equal]: Equal[scalaz72.ParIO[Any, E, A]] =
    ioEqual[E, A].contramap(Tag.unwrap)

  implicit def ioParArbitrary[E: Arbitrary: Cogen, A: Arbitrary: Cogen]: Arbitrary[scalaz72.ParIO[Any, E, A]] =
    Arbitrary(genIO[E, A].map(Tag.apply))
}

trait GenIO {
  /**
   * Given a generator for `A`, produces a generator for `IO[E, A]` using the `IO.point` constructor.
   */
  def genSyncSuccess[E, A: Arbitrary]: Gen[IO[E, A]] = Arbitrary.arbitrary[A].map(IO.succeed[A](_))

  /**
   * Given a generator for `A`, produces a generator for `IO[E, A]` using the `IO.async` constructor.
   */
  def genAsyncSuccess[E, A: Arbitrary]: Gen[IO[E, A]] =
    Arbitrary.arbitrary[A].map(a => IO.effectAsync[E, A](k => k(IO.succeed(a))))

  /**
   * Randomly uses either `genSyncSuccess` or `genAsyncSuccess` with equal probability.
   */
  def genSuccess[E, A: Arbitrary]: Gen[IO[E, A]] = Gen.oneOf(genSyncSuccess[E, A], genAsyncSuccess[E, A])

  /**
   * Given a generator for `E`, produces a generator for `IO[E, A]` using the `IO.fail` constructor.
   */
  def genSyncFailure[E: Arbitrary, A]: Gen[IO[E, A]] = Arbitrary.arbitrary[E].map(IO.fail[E])

  /**
   * Given a generator for `E`, produces a generator for `IO[E, A]` using the `IO.async` constructor.
   */
  def genAsyncFailure[E: Arbitrary, A]: Gen[IO[E, A]] =
    Arbitrary.arbitrary[E].map(err => IO.effectAsync[E, A](k => k(IO.fail(err))))

  /**
   * Randomly uses either `genSyncFailure` or `genAsyncFailure` with equal probability.
   */
  def genFailure[E: Arbitrary, A]: Gen[IO[E, A]] = Gen.oneOf(genSyncFailure[E, A], genAsyncFailure[E, A])

  /**
   * Randomly uses either `genSuccess` or `genFailure` with equal probability.
   */
  def genIO[E: Arbitrary, A: Arbitrary]: Gen[IO[E, A]] =
    Gen.oneOf(genSuccess[E, A], genFailure[E, A])
}
