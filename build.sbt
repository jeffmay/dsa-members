name := "dsa-members-root"

ThisBuild / organization := "org.dsasf"
ThisBuild / organizationName := "San Francisco Democratic Socialists of America"

ThisBuild / scalaVersion := "3.2.1"

// Avoid the error about unused key, since this is used by the IDE and not SBT
Global / excludeLintKeys += idePackagePrefix

// Reload the sbt config and .env environment variables when the build configs change
Global / onChangedBuildSource := ReloadOnSourceChanges

val commonScalacOptions = scalacOptions ++= Seq(
  "-explain",
  "-deprecation",
)

def commonLibrary(
  id: String,
  dependencies: Deps,
  packagePrefix: String,
): Project = {
  Project(id, file(id)).settings(
    name := id,
    // Set the package prefix for the IDE, so that we don't have to use deeply nested directories
    idePackagePrefix := Some(packagePrefix),
    libraryDependencies ++= dependencies.libraries,
    resolvers ++= dependencies.resolvers,
    commonScalacOptions,
    // Add support for Scala 3-like type lambda syntax
//    addCompilerPlugin(
//      "org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full,
//    ),
  )
}

lazy val csv = commonLibrary("zio-csv", Dependencies.csv, "zio")
  .dependsOn(enumOps)
//  .dependsOn(enumeratumOps)

// lazy val csvInteropCats =
// commonLibrary("zio-csv-interop-cats", Dependencies.csvInteropCats, "zio")
// .dependsOn(csv)

// TODO: Remove after upgrade to Scala 3
// lazy val enumeratumOps =
//   commonLibrary("enumeratum-ops", Dependencies.enumeratumOps, "enumeratum")
//     .settings(scalaVersion := "2.13.10")

lazy val enumOps = commonLibrary("enum-ops", Dependencies.enumOps, "zio.util")
  .settings(
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
  )

def commonProject(
  id: String,
  dependencies: Deps,
): Project =
  Project(id, file(id))
    .enablePlugins(BuildInfoPlugin)
    .settings(
      name := s"dsa-members-$id",
      buildInfoKeys := Seq[BuildInfoKey](
        name,
        version,
        scalaVersion,
        sbtVersion,
      ),
      buildInfoOptions ++= Seq(
        BuildInfoOption.BuildTime,
        BuildInfoOption.ToJson,
      ),
      buildInfoPackage := s"org.dsasf.members.${id.replaceAllLiterally("-", ".")}",
      // Set the package prefix for the IDE, so that we don't have to use deeply nested directories
      idePackagePrefix := Some("org.dsasf.members"),
      libraryDependencies ++= dependencies.libraries,
      resolvers ++= dependencies.resolvers,
      commonScalacOptions,
    )

lazy val api = commonProject("api", Dependencies.api)
  .dependsOn(database)

lazy val database = commonProject("database", Dependencies.database)
  .dependsOn(
    databaseCommon,
    models,
  )

lazy val databaseCommon =
  commonProject("database-common", Dependencies.databaseCommon)

lazy val models =
  commonProject("models", Dependencies.models)

lazy val jobs = commonProject("jobs", Dependencies.jobs)
  .dependsOn(
    csv,
    database,
  )
