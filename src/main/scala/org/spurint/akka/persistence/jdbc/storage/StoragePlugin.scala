package org.spurint.akka.persistence.jdbc.storage

import java.util.Locale
import scala.util.{Failure, Success, Try}

private[jdbc] object StoragePlugin {
  def fromJdbcUrl(url: String): Try[StoragePlugin] = {
    val parts = url.split(":")
    if (parts.length > 1 && parts(0).toLowerCase(Locale.US) == "jdbc") {
      parts(1).toLowerCase(Locale.US) match {
        case "mysql" => Success(MySqlStoragePlugin)
        case x => Failure(new IllegalArgumentException(s"Unsupported database type: $x"))
      }
    } else {
      Failure(new IllegalArgumentException(s"Invalid JDBC URL: $url"))
    }
  }
}

private[jdbc] trait StoragePlugin {
  def createMetadataTable(tableName: String): String
  def setHighestSequenceNr(tableName: String): String
  def fetchHighestSequenceNrMeta(tableName: String): String

  def createJournalTable(tableName: String): String
  def insertJournalEntry(tableName: String): String
  def fetchJournalEntries(tableName: String, limit: Long): String
  def deleteJournalEntries(tableName: String): String
  def fetchHighestSequenceNr(tableName: String): String

  def createSnapshotTable(tableName: String): String
  def saveSnapshot(tableName: String): String
  def loadSnapshot(tableName: String): String
  def deleteSnapshot(tableName: String): String
  def deleteSnapshots(tableName: String): String
}
