package org.spurint.akka.persistence.jdbc

import akka.actor.ActorContext
import akka.serialization.{Serialization, SerializationExtension}
import com.typesafe.config.Config
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import org.spurint.akka.persistence.jdbc.storage.StoragePlugin
import scala.util.Try

private[jdbc] abstract class JdbcPluginBase[T <: JdbcPluginConfigBase](rawConfig: Config) {
  def context: ActorContext
  def createConfig: Config => T
  def createTableStatements: Seq[String]

  protected lazy val serialization: Serialization = SerializationExtension(context.system)
  protected val config: T = createConfig(rawConfig)
  protected val storagePlugin: StoragePlugin = StoragePlugin.fromJdbcUrl(config.jdbcUrl).get
  protected val dataSource: HikariDataSource = {
    val hcfg = new HikariConfig()
    hcfg.setDriverClassName(config.driverClassName)
    hcfg.setJdbcUrl(config.jdbcUrl)
    val ds = new HikariDataSource(hcfg)
    sys.addShutdownHook(ds.close())
    ds
  }

  Try(dataSource.getConnection).flatMap { conn =>
    val result = Try {
      conn.setAutoCommit(true)
      val stmt = conn.createStatement()
      createTableStatements.foreach(stmt.execute)
      stmt.close()
    }
    Try(conn.close())
    result
  }.get
}
