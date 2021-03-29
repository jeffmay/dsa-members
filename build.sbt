name := "dsa-members-root"

ThisBuild / organization := "org.dsasf"
ThisBuild / organizationName := "San Francisco Democratic Socialists of America"

ThisBuild / scalaVersion := "2.13.5"
//ThisBuild / scalaVersion := "3.0.0-RC1"
//ThisBuild / useScala3doc := true

// Avoid the error about unused key, since this is used by the IDE and not SBT
Global / excludeLintKeys += idePackagePrefix

def commonProject(id: String, dependencies: Deps): Project =
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
      buildInfoPackage := s"org.dsasf.members.$id",
      // Set the package prefix for the IDE, so that we don't have to use deeply nested directories
      idePackagePrefix := Some("org.dsasf.members"),
      libraryDependencies ++= dependencies.libraries,
      resolvers ++= dependencies.resolvers,
    )

lazy val api = commonProject("api", Dependencies.api)
  .dependsOn(database)

lazy val database = commonProject("database", Dependencies.database)

lazy val jobs = commonProject("jobs", Dependencies.importer)
  .dependsOn(database)
