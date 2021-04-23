package zio
package csv.interop

import munit.ScalaCheckSuite
import org.scalacheck.rng.Seed

trait ImplicitScalaCheckSeedForSuite {
  self: ScalaCheckSuite =>

  /** Override the initial seed to be a `val` so that it is fixed for the whole suite.
    */
  override protected val scalaCheckInitialSeed: String =
    scalaCheckTestParameters.initialSeed.getOrElse {
      Seed.random()
    }.toBase64

  /** Sets the seed based on the defined `val`s, or fix it to a random value for the life of this suite.
    *
    * @note a better approach would be to provide a seed that lives for the life of a single test,
    *       but I would have to figure out how to change all the signatures to provide an implicit
    *       test context.
    */
  implicit final protected lazy val initialSeed: Seed =
    scalaCheckTestParameters.initialSeed.getOrElse {
      Seed.fromBase64(scalaCheckInitialSeed).get
    }
}
