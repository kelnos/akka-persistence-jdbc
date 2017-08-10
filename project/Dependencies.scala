import sbt._

object Dependencies {
  lazy val versions = Map(
    "akka" -> "2.5.3",
    "hikariCP" -> "2.6.2",
    "scalatest" -> "3.0.3",
    "mysql-connector-java" -> "5.1.38"
  )

  lazy val akkaPersistence = "com.typesafe.akka" %% "akka-persistence" % versions("akka")
  lazy val hikariCP = "com.zaxxer" % "HikariCP" % versions("hikariCP")

  lazy val scalaTest = "org.scalatest" %% "scalatest" % versions("scalatest") % Test
  lazy val akkaPersistenceTk = "com.typesafe.akka" %% "akka-persistence-tck" % versions("akka") % Test

  lazy val mysqlConnectorJava = "mysql" % "mysql-connector-java" % versions("mysql-connector-java") % Test
}
