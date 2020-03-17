package zio.interop

import zio.IO
import zio.random.Random
import zio.test.Gen

trait GenIO {

  /**
   * Given a generator for `A`, produces a generator for `IO[E, A]` using the `IO.point` constructor.
   */
  def genSyncSuccess[E, A](implicit gen: Gen[Random, A]): Gen[Random, IO[E, A]] =
    gen.map((a: A) => IO.succeed(a))

  /**
   * Given a generator for `A`, produces a generator for `IO[E, A]` using the `IO.async` constructor.
   */
  def genAsyncSuccess[E, A](implicit gen: Gen[Random, A]): Gen[Random, IO[E, A]] =
    gen.map(a => IO.effectAsync[E, A](k => k(IO.succeed(a))))

  /**
   * Randomly uses either `genSyncSuccess` or `genAsyncSuccess` with equal probability.
   */
  def genSuccess[E, A](implicit gen: Gen[Random, A]): Gen[Random, IO[E, A]] =
    Gen.oneOf(genSyncSuccess, genAsyncSuccess)

  /**
   * Given a generator for `E`, produces a generator for `IO[E, A]` using the `IO.fail` constructor.
   */
  def genSyncFailure[E, A](implicit gen: Gen[Random, E]): Gen[Random, IO[E, A]] =
    gen.map((e: E) => IO.fail(e))

  /**
   * Given a generator for `E`, produces a generator for `IO[E, A]` using the `IO.async` constructor.
   */
  def genAsyncFailure[E, A](implicit gen: Gen[Random, E]): Gen[Random, IO[E, A]] =
    gen.map(a => IO.effectAsync[E, A](k => k(IO.fail(a))))

  /**
   * Randomly uses either `genSyncFailure` or `genAsyncFailure` with equal probability.
   */
  def genFailure[E, A](implicit gen: Gen[Random, E]): Gen[Random, IO[E, A]] =
    Gen.oneOf(genSyncFailure, genAsyncFailure)

  /**
   * Randomly uses either `genSuccess` or `genFailure` with equal probability.
   */
  implicit def genIO[E, A](implicit gen1: Gen[Random, A], gen2: Gen[Random, E]): Gen[Random, IO[E, A]] =
    Gen.oneOf(genSuccess, genFailure)
}
