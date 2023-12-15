ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.12.8"

lazy val root = (project in file("."))
  .settings(
    name := "bigdata_lab4"
  )

libraryDependencies += "org.apache.zookeeper" % "zookeeper" % "3.4.13" % "compile"
