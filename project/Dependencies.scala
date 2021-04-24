import sbt._

case class Deps(
  libraries: Seq[ModuleID],
  resolvers: Seq[Resolver] = Nil,
)

object Dependencies {

  final private val catsVersion = "2.6.0"
  final private val catsEffectVersion = "3.1.0"
  final private val disciplineVersion = "1.1.4"
  final private val doobieVersion = "1.0.0-M1"
  final private val enumeratumVersion = "1.6.1"
  final private val fs2Version = "3.0.2"
  final private val munitVersion = "0.7.25"
  final private val munitDisciplineVersion = "1.0.7"
  final private val newtypeVersion = "0.4.4"
  final private val refinedVersion = "0.9.24"
  final private val scalaCheckVersion = "1.15.3"
  final private val scalaCsvVersion = "1.3.7"
  final private val scalacticVersion = "3.2.7"
  final private val zioVersion = "1.0.5"
  final private val zioCatsVersion = "2.4.0.0"
  final private val zioConfigVersion = "1.0.4"
  final private val zioHttpVersion = "1.0.0.0-RC15"
  final private val zioJsonVersion = "0.1.3"
  final private val zioLoggingVersion = "0.5.8"
  final private val zioNioVersion = "1.0.0-RC10"

  private val catsCore = "org.typelevel" %% "cats-core" % catsVersion
  private val catsLaws = "org.typelevel" %% "cats-laws" % catsVersion
  private val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
  private val discipline =
    "org.typelevel" %% "discipline-core" % disciplineVersion
  private val doobieCore = "org.tpolecat" %% "doobie-core" % doobieVersion
  private val doobiePostgres =
    "org.tpolecat" %% "doobie-postgres" % doobieVersion
  private val enumeratum = "com.beachape" %% "enumeratum" % enumeratumVersion
  private val fs2Core = "co.fs2" %% "fs2-core" % fs2Version
  private val munit = "org.scalameta" %% "munit" % munitVersion
  private val munitDiscipline =
    "org.typelevel" %% "discipline-munit" % munitDisciplineVersion
  private val newtype = "io.estatico" %% "newtype" % newtypeVersion
  private val refined = "eu.timepit" %% "refined" % refinedVersion
  private val scalaCheck = "org.scalacheck" %% "scalacheck" % scalaCheckVersion
  private val scalaCsv = "com.github.tototoshi" %% "scala-csv" % scalaCsvVersion
  private val scalactic = "org.scalactic" %% "scalactic" % scalacticVersion
  private val zio = "dev.zio" %% "zio" % zioVersion
  private val zioCats = "dev.zio" %% "zio-interop-cats" % zioCatsVersion
  private val zioConfig = "dev.zio" %% "zio-config" % zioConfigVersion
  private val zioConfigMagnolia =
    "dev.zio" %% "zio-config-magnolia" % zioConfigVersion
  private val zioHttp = "io.d11" %% "zhttp" % zioHttpVersion
  private val zioJson = "dev.zio" %% "zio-json" % zioJsonVersion
  private val zioLogging = "dev.zio" %% "zio-logging" % zioLoggingVersion
  private val zioNio = "dev.zio" %% "zio-nio" % zioNioVersion
  private val zioStreams = "dev.zio" %% "zio-streams" % zioVersion

  final private val common = Deps(
    libraries = Seq(
      enumeratum,
      newtype,
      refined,
      zio,
      zioConfig,
      zioConfigMagnolia,
      zioLogging,
    ) ++ Seq(
      // Test-only dependencies
      munit,
    ).map(_ % Test),
  )

  final val api = Deps(
    libraries = common.libraries ++ Seq(
//      doobieCore,
//      fs2Core,
      zioHttp,
      zioJson,
      zioStreams,
    ),
  )

  // this does not share common deps because it is intended to be a shared library
  final val csv = Deps(
    libraries = Seq(
      catsCore,
      scalaCsv,
      zio,
      zioNio,
    ) ++ Seq(
      // Test-only dependencies
      munit,
    ).map(_ % Test),
  )

  // this does not share common deps because it is intended to be a shared library
  final val csvInteropCats = Deps(
    libraries = csv.libraries ++ Seq(
      zioCats,
    ) ++ Seq(
      // Test-only dependencies
      catsLaws,
      discipline,
      munitDiscipline,
      scalaCheck,
    ).map(_ % Test),
  )

  final val enumeratumOps = Deps(
    libraries = Seq(
      enumeratum,
    ) ++ Seq(
      // Test-only dependencies
      munit,
    ).map(_ % Test),
  )

  final val database = Deps(
    libraries = common.libraries ++ Seq(
      catsCore,
      catsEffect,
      doobieCore,
      doobiePostgres,
      fs2Core,
    ),
  )

  final val jobs = Deps(
    libraries = common.libraries ++ Seq(
      scalactic,
      zioNio,
      zioStreams,
    ),
  )
}
