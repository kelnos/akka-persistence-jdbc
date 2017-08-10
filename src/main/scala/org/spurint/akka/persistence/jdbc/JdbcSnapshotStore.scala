package org.spurint.akka.persistence.jdbc

import akka.persistence.serialization.Snapshot
import akka.persistence.snapshot.SnapshotStore
import akka.persistence.{SelectedSnapshot, SnapshotMetadata, SnapshotSelectionCriteria}
import com.typesafe.config.Config
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class JdbcSnapshotStore(rawConfig: Config) extends JdbcPluginBase[JdbcSnapshotStoreConfig](rawConfig) with SnapshotStore {
  override def createConfig: Config => JdbcSnapshotStoreConfig = JdbcSnapshotStoreConfig.apply

  override def createTableStatements: Seq[String] = Seq(
    storagePlugin.createSnapshotTable(config.snapshotTableName)
  )

  override def saveAsync(metadata: SnapshotMetadata, snapshot: Any): Future[Unit] = {
    Future.fromTry {
      Try(dataSource.getConnection).flatMap { conn =>
        val result = Try {
          conn.setAutoCommit(true)
          val stmt = conn.prepareStatement(storagePlugin.saveSnapshot(config.snapshotTableName))
          stmt.closeOnCompletion()
          stmt.setString(1, metadata.persistenceId)
          stmt.setLong(2, metadata.sequenceNr)
          stmt.setLong(3, metadata.timestamp)
          stmt.setLong(5, metadata.timestamp)

          serialization.serialize(Snapshot(snapshot)) match {
            case Failure(e) =>
              stmt.close()
              throw e
            case Success(serialized) =>
              val blob = conn.createBlob()
              blob.setBytes(1, serialized)
              stmt.setBlob(4, blob)
              stmt.setBlob(6, blob)
          }

          stmt.executeUpdate()
          stmt.close()
        }
        Try(conn.close())
        result
      }
    }
  }

  override def loadAsync(persistenceId: String, criteria: SnapshotSelectionCriteria): Future[Option[SelectedSnapshot]] = {
    Future.fromTry {
      Try(dataSource.getConnection).flatMap { conn =>
        val result = Try {
          val stmt = conn.prepareStatement(storagePlugin.loadSnapshot(config.snapshotTableName))
          stmt.closeOnCompletion()
          stmt.setString(1, persistenceId)
          stmt.setLong(2, criteria.minSequenceNr)
          stmt.setLong(3, criteria.maxSequenceNr)
          stmt.setLong(4, criteria.minTimestamp)
          stmt.setLong(5, criteria.maxTimestamp)
          val resultSet = stmt.executeQuery()
          if (!resultSet.first()) {
            None
          } else {
            val sequenceNr = resultSet.getLong(1)
            val timestamp = resultSet.getLong(2)
            val blob = resultSet.getBlob(3)
            val bytes = blob.getBytes(1, blob.length.toInt)
            stmt.close()
            val snapshot = serialization.deserialize(bytes, classOf[Snapshot])
            Some(SelectedSnapshot(SnapshotMetadata(persistenceId, sequenceNr, timestamp), snapshot.get.data))
          }
        }
        Try(conn.close())
        result
      }
    }
  }

  override def deleteAsync(metadata: SnapshotMetadata): Future[Unit] = {
    Future.fromTry {
      Try(dataSource.getConnection).flatMap { conn =>
        val result = Try {
          conn.setAutoCommit(true)
          val stmt = conn.prepareStatement(storagePlugin.deleteSnapshot(config.snapshotTableName))
          stmt.closeOnCompletion()
          stmt.setString(1, metadata.persistenceId)
          stmt.setLong(2, metadata.sequenceNr)
          stmt.executeUpdate()
          stmt.close()
        }
        Try(conn.close())
        result
      }
    }
  }

  override def deleteAsync(persistenceId: String, criteria: SnapshotSelectionCriteria): Future[Unit] = {
    Future.fromTry {
      Try(dataSource.getConnection).flatMap { conn =>
        val result = Try {
          conn.setAutoCommit(true)
          val stmt = conn.prepareStatement(storagePlugin.deleteSnapshots(config.snapshotTableName))
          stmt.closeOnCompletion()
          stmt.setString(1, persistenceId)
          stmt.setLong(2, criteria.minSequenceNr)
          stmt.setLong(3, criteria.maxSequenceNr)
          stmt.setLong(4, criteria.minTimestamp)
          stmt.setLong(5, criteria.maxTimestamp)
          stmt.executeUpdate()
          stmt.close()
        }
        Try(conn.close())
        result
      }
    }
  }
}
