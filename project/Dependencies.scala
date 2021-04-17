import sbt._

case class Deps(
  libraries: Seq[ModuleID],
  resolvers: Seq[Resolver] = Nil,
)

object Dependencies {

  final private val catsVersion = "2.5.0"
  //  private final val catsEffectVersion = "2.3.3"
  // private final val catsEffectVersion = "3.0.0-RC2"
  final private val doobieVersion = "0.12.1"
  final private val enumeratumVersion = "1.6.1"
  final private val fs2Version = "2.5.3"
  // private final val fs2Version = "3.0.0-M9"
  final private val munitVersion = "0.7.22"
  final private val scalaCsvVersion = "1.3.7"
  final private val zioVersion = "1.0.5"
  final private val zioCatsVersion = "2.4.0.0"
  final private val zioConfigVersion = "1.0.2"
  final private val zioHttpVersion = "1.0.0.0-RC13"
  final private val zioJsonVersion = "0.1.3"
  final private val zioLoggingVersion = "0.5.8"
  final private val zioNioVersion = "1.0.0-RC10"

  private val fs2Core = "co.fs2" %% "fs2-core" % fs2Version
  private val catsCore = "org.typelevel" %% "cats-core" % catsVersion
  //  private val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
  private val doobieCore = "org.tpolecat" %% "doobie-core" % doobieVersion
  private val enumeratum = "com.beachape" %% "enumeratum" % enumeratumVersion
  private val munit = "org.scalameta" %% "munit" % munitVersion
  private val scalaCsv = "com.github.tototoshi" %% "scala-csv" % scalaCsvVersion
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
      doobieCore,
      fs2Core,
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
      zioCats,
      zioNio,
    ) ++ Seq(
      // Test-only dependencies
      munit,
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
      doobieCore,
      fs2Core,
    ),
  )

  final val importer = Deps(
    libraries = common.libraries ++ Seq(
      zioNio,
      zioStreams,
    ),
  )
}
