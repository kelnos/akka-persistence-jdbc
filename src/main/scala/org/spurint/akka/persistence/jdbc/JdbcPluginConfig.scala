package org.spurint.akka.persistence.jdbc

import com.typesafe.config.Config

private[jdbc] abstract class JdbcPluginConfigBase(config: Config) {
  val jdbcUrl: String = config.getString("jdbc-url")
  val driverClassName: String = config.getString("driver-class-name")
}

class JdbcJournalConfig private (config: Config) extends JdbcPluginConfigBase(config) {
  val metadataTableName: String = config.getString("metadata-table-name")
  val journalTableName: String = config.getString("journal-table-name")
}

object JdbcJournalConfig {
  def apply(config: Config): JdbcJournalConfig = new JdbcJournalConfig(config)
}

class JdbcSnapshotStoreConfig private (config: Config) extends JdbcPluginConfigBase(config) {
  val snapshotTableName: String = config.getString("snapshot-table-name")
}

object JdbcSnapshotStoreConfig {
  def apply(config: Config): JdbcSnapshotStoreConfig = new JdbcSnapshotStoreConfig(config)
}
