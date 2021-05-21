package zio.interop

import zio.{ IO }
import zio.random.Random
import zio.test._
import zio.test.Gen._
import scalaz._
import _root_.scalaz._
import _root_.scalaz.Scalaz._

object ScalazPropertiesSpec extends DefaultRunnableSpec with GenIO {

  private val rts = zio.Runtime.default

  implicit def ioEqual[E: Equal, A: Equal]: Equal[IO[E, A]] =
    new Equal[IO[E, A]] {
      override def equal(io1: IO[E, A], io2: IO[E, A]): Boolean =
        rts.unsafeRun(io1.either) === rts.unsafeRun(io2.either)
    }

  implicit def ioParEqual[E: Equal, A: Equal]: Equal[ParIO[Any, E, A]] =
    new Equal[ParIO[Any, E, A]] {
      override def equal(io1: ParIO[Any, E, A], io2: ParIO[Any, E, A]): Boolean =
        rts.unsafeRun(Tag.unwrap(io1).either) === rts.unsafeRun(Tag.unwrap(io2).either)
    }

  implicit val intGen: Gen[Random, Int]                                                    = anyInt
  implicit val optionalIntGen: Gen[Random, Option[Unit]]                                   = zio.test.Gen.option(unit)
  implicit def intFn: Gen[Random, Int => Int]                                              = Gen.function(Gen.anyInt)
  implicit def intGenIOFn[A](implicit gen: Gen[Random, A]): Gen[Random, Int => IO[A, Int]] = Gen.function(genIO[A, Int])
  implicit val intGenIOFn2: Gen[Random, IO[Int, Int => Int]]                               = genIO(intFn, Gen.anyInt)
  implicit def intGenIO: Gen[Random, IO[Int, Int]]                                         = genIO[Int, Int]

  implicit val ioParGen: Gen[Random, ParIO[Any, Int, Int]]           = intGenIO.map(Tag.apply)
  implicit val ioParGenFn2: Gen[Random, ParIO[Any, Int, Int => Int]] = intGenIOFn2.map(Tag.apply)

  override def spec = suite("scalazPlatform72Spec")(
    suite("laws must hold for")(
      ScalazProperties.bifunctor.laws[IO],
      ScalazProperties.bindRec.laws[IO[Int, ?]],
      ScalazProperties.plus.laws[IO[Int, ?]],
      ScalazProperties.monadPlus.laws[IO[Int, ?]]("monad plus"),
      ScalazProperties.monadPlus.laws[IO[Option[Unit], ?]]("monad plus (monoid)"),
      ScalazProperties.monadError.laws[IO[Int, ?], Int],
      ScalazProperties.applicative.laws[ParIO[Any, Int, ?]]
    )
  )
}
