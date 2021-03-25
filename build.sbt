organization := "org.dsasf"
organizationName := "San Francisco Democratic Socialists of America"

name := "dsa-members"

scalaVersion := "3.0.0-RC1"
useScala3doc := true

enablePlugins(BuildInfoPlugin)
buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion)
buildInfoPackage := "org.dsasf.members"
