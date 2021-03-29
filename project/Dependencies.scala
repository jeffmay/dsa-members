import sbt._

case class Deps(
  libraries: Seq[ModuleID],
  resolvers: Seq[Resolver] = Nil,
)

object Dependencies {

//  private final val catsVersion = "2.4.2"
//  private final val catsEffectVersion = "2.3.3"
  // private final val catsEffectVersion = "3.0.0-RC2"
  private final val doobieVersion = "0.12.1"
  private final val fs2Version = "2.5.3"
  // private final val fs2Version = "3.0.0-M9"
  private final val munitVersion = "0.7.22"
  private final val zioVersion = "1.0.5"
  private final val zioConfigVersion = "1.0.2"
  private final val zioHttpVersion = "1.0.0.0-RC13"
  private final val zioJsonVersion = "0.1.3"
  private final val zioLoggingVersion = "0.5.8"
  private final val zioNioVersion = "1.0.0-RC10"

  private val fs2Core = "co.fs2" %% "fs2-core" % fs2Version
//  private val catsCore = "org.typelevel" %% "cats-core" % catsVersion
//  private val catsEffect = "org.typelevel" %% "cats-effect" % catsEffectVersion
  private val doobieCore = "org.tpolecat" %% "doobie-core" % doobieVersion
  private val munit = "org.scalameta" %% "munit" % munitVersion
  private val zio = "dev.zio" %% "zio" % zioVersion
  private val zioConfig = "dev.zio" %% "zio-config" % zioConfigVersion
  private val zioConfigMagnolia = "dev.zio" %% "zio-config-magnolia" % zioConfigVersion
  private val zioHttp = "io.d11" %% "zhttp" % zioHttpVersion
  private val zioJson = "dev.zio" %% "zio-json" % zioJsonVersion
  private val zioLogging = "dev.zio" %% "zio-logging" % zioLoggingVersion
  private val zioNio = "dev.zio" %% "zio-nio" % zioNioVersion
  private val zioStreams = "dev.zio" %% "zio-streams" % zioVersion

  private final val common = Deps(
    libraries = Seq(
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
