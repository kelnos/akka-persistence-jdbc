import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "org.spurint",
      version := "0.1.0-SNAPSHOT",
      scalaVersion := "2.12.2",
      crossScalaVersions := Seq("2.11.11", "2.12.2")
    )),
    name := "akka-persistence-jdbc",
    libraryDependencies ++= Seq(
      akkaPersistence,
      hikariCP,

      scalaTest,
      akkaPersistenceTk,
      mysqlConnectorJava
    )
  )
