package org.spurint.akka.persistence.jdbc.storage

private[jdbc] object MySqlStoragePlugin extends StoragePlugin {
  override def createMetadataTable(tableName: String): String =
    s"""CREATE TABLE IF NOT EXISTS `$tableName` (
       |  `AutoId` INT UNSIGNED NOT NULL AUTO_INCREMENT,
       |  `PersistenceId` VARCHAR(255) NOT NULL,
       |  `HighestSequenceNr` BIGINT NOT NULL,
       |  PRIMARY KEY (`AutoId`),
       |  UNIQUE KEY `Key_PersistenceId` (`PersistenceId`)
       | ) ENGINE=InnoDB DEFAULT CHARSET=utf8
     """.stripMargin

  override def setHighestSequenceNr(tableName: String): String =
    s"INSERT INTO `$tableName` (`PersistenceId`, `HighestSequenceNr`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `HighestSequenceNr` = ?"

  override def fetchHighestSequenceNrMeta(tableName: String): String =
    s"SELECT `HighestSequenceNr` FROM `$tableName` WHERE `PersistenceId` = ?"

  override def createJournalTable(tableName: String): String =
    s"""CREATE TABLE IF NOT EXISTS `$tableName` (
       |  `AutoId` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
       |  `PersistenceId` VARCHAR(255) NOT NULL,
       |  `SequenceNr` BIGINT NOT NULL,
       |  `Message` LONGBLOB NOT NULL,
       |  PRIMARY KEY (`AutoId`),
       |  UNIQUE KEY `Key_PersistenceIdSequenceNr` (`PersistenceId`, `SequenceNr`)
       |) ENGINE=InnoDB DEFAULT CHARSET=utf8
     """.stripMargin

  override def insertJournalEntry(tableName: String): String =
    s"INSERT INTO `$tableName` (`PersistenceId`, `SequenceNr`, `Message`) VALUES (?, ?, ?)"

  override def fetchJournalEntries(tableName: String, limit: Long): String =
    s"SELECT `Message` FROM `$tableName` WHERE `PersistenceId` = ? AND `SequenceNr` >= ? AND `SequenceNr` <= ? ORDER BY `SequenceNr` LIMIT $limit"

  override def deleteJournalEntries(tableName: String): String =
    s"DELETE FROM `$tableName` WHERE `PersistenceId` = ? AND `SequenceNr` <= ?"

  override def fetchHighestSequenceNr(tableName: String): String =
    s"SELECT `SequenceNr` FROM `$tableName` WHERE `PersistenceId` = ? ORDER BY `SequenceNr` DESC LIMIT 1"

  override def createSnapshotTable(tableName: String): String =
    s"""CREATE TABLE IF NOT EXISTS `$tableName` (
       |  `AutoId` INT UNSIGNED NOT NULL AUTO_INCREMENT,
       |  `PersistenceId` VARCHAR(255) NOT NULL,
       |  `SequenceNr` BIGINT NOT NULL,
       |  `Timestamp` BIGINT NOT NULL,
       |  `Snapshot` LONGBLOB NOT NULL,
       |  PRIMARY KEY (`AutoId`),
       |  UNIQUE KEY `Key_PersistenceIdSequenceNr` (`PersistenceId`, `SequenceNr`)
       |) ENGINE=InnoDB DEFAULT CHARSET=utf8
     """.stripMargin

  override def saveSnapshot(tableName: String): String =
    s"INSERT INTO `$tableName` (`PersistenceId`, `SequenceNr`, `Timestamp`, `Snapshot`) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE `Timestamp` = ?, `Snapshot` = ?"

  override def loadSnapshot(tableName: String): String =
    s"""SELECT `SequenceNr`, `Timestamp`, `Snapshot` FROM `$tableName` WHERE
       |  `PersistenceId` = ? AND
       |  `SequenceNr` >= ? AND
       |  `SequenceNr` <= ? AND
       |  `Timestamp` >= ? AND
       |  `Timestamp` <= ?
       |ORDER BY `SequenceNr` DESC LIMIT 1
     """.stripMargin

  override def deleteSnapshot(tableName: String): String =
    s"DELETE FROM `$tableName` WHERE `PersistenceId` = ? AND `SequenceNr` = ?"

  override def deleteSnapshots(tableName: String): String =
    s"""DELETE FROM `$tableName` WHERE
       |  `PersistenceId` = ? AND
       |  `SequenceNr` >= ? AND
       |  `SequenceNr` <= ? AND
       |  `Timestamp` >= ? AND
       |  `Timestamp` <= ?
     """.stripMargin
}
