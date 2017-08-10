package org.spurint.akka.persistence.jdbc

import akka.persistence.journal.AsyncWriteJournal
import akka.persistence.{AtomicWrite, PersistentRepr}
import com.typesafe.config.Config
import scala.collection.immutable
import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

class JdbcJournal(rawConfig: Config) extends JdbcPluginBase[JdbcJournalConfig](rawConfig) with AsyncWriteJournal {
  private implicit val ec = context.dispatcher

  override def createConfig: Config => JdbcJournalConfig = JdbcJournalConfig.apply

  override def createTableStatements: Seq[String] = Seq(
    storagePlugin.createMetadataTable(config.metadataTableName),
    storagePlugin.createJournalTable(config.journalTableName)
  )

  override def asyncWriteMessages(messages: immutable.Seq[AtomicWrite]): Future[immutable.Seq[Try[Unit]]] = {
    Future.sequence(messages.map { message =>
      Future {
        Try(dataSource.getConnection).flatMap { conn =>
          val result = Try {
            conn.setAutoCommit(false)

            val insertStmt = {
              val stmt = conn.prepareStatement(storagePlugin.insertJournalEntry(config.journalTableName))
              stmt.closeOnCompletion()

              message.payload.foreach { repr =>
                serialization.serialize(repr) match {
                  case Failure(e) =>
                    stmt.close()
                    throw e
                  case Success(serialized) =>
                    stmt.setString(1, repr.persistenceId)
                    stmt.setLong(2, repr.sequenceNr)
                    val blob = conn.createBlob()
                    blob.setBytes(1, serialized)
                    stmt.setBlob(3, blob)
                    stmt.addBatch()
                }
              }

              stmt
            }

            val upsertStmt = {
              val stmt = conn.prepareStatement(storagePlugin.setHighestSequenceNr(config.metadataTableName))
              stmt.closeOnCompletion()

              message.payload.groupBy(_.persistenceId).foreach { case (persistenceId, reprs) =>
                val highest = reprs.map(_.sequenceNr).max
                stmt.setString(1, persistenceId)
                stmt.setLong(2, highest)
                stmt.setLong(3, highest)
                stmt.addBatch()
              }

              stmt
            }

            insertStmt.executeBatch()
            upsertStmt.executeBatch()
            conn.commit()
            insertStmt.close()
            upsertStmt.close()
          }.recoverWith {
            case NonFatal(e) =>
              conn.rollback()
              Failure(e)
          }
          Try(conn.close())
          result
        }
      }
    })
  }

  override def asyncDeleteMessagesTo(persistenceId: String, toSequenceNr: Long): Future[Unit] = {
    Future.fromTry {
      Try(dataSource.getConnection).flatMap { conn =>
        val result = Try {
          conn.setAutoCommit(true)
          val stmt = conn.prepareStatement(storagePlugin.deleteJournalEntries(config.journalTableName))
          stmt.closeOnCompletion()
          stmt.setString(1, persistenceId)
          stmt.setLong(2, toSequenceNr)
          stmt.executeUpdate()
          stmt.close()
        }
        Try(conn.close())
        result
      }
    }
  }

  override def asyncReadHighestSequenceNr(persistenceId: String, fromSequenceNr: Long): Future[Long] = {
    Future.fromTry {
      Try(dataSource.getConnection).flatMap { conn =>
        val result = Try {
          val stmt = conn.prepareStatement(storagePlugin.fetchHighestSequenceNrMeta(config.metadataTableName))
          stmt.closeOnCompletion()
          stmt.setString(1, persistenceId)
          val resultSet = stmt.executeQuery()
          val highest = if (!resultSet.first()) {
            fromSequenceNr
          } else {
            val metaHighest = resultSet.getLong(1)

            val stmt1 = conn.prepareStatement(storagePlugin.fetchHighestSequenceNr(config.journalTableName))
            stmt1.closeOnCompletion()
            stmt1.setString(1, persistenceId)
            val resultSet1 = stmt1.executeQuery()
            val actualHighest = if (!resultSet1.first()) {
              metaHighest.max(fromSequenceNr)
            } else {
              val journalHighest = resultSet1.getLong(1)
              journalHighest.max(metaHighest).max(fromSequenceNr)
            }
            stmt1.close()
            actualHighest
          }
          stmt.close()
          highest
        }
        Try(conn.close())
        result
      }
    }
  }

  override def asyncReplayMessages(persistenceId: String, fromSequenceNr: Long, toSequenceNr: Long, max: Long)(recoveryCallback: (PersistentRepr) => Unit): Future[Unit] = {
    Future.fromTry {
      Try(dataSource.getConnection).flatMap { conn =>
        val result = Try {
          val stmt = conn.prepareStatement(storagePlugin.fetchJournalEntries(config.journalTableName, max))
          stmt.closeOnCompletion()
          stmt.setString(1, persistenceId)
          stmt.setLong(2, fromSequenceNr)
          stmt.setLong(3, toSequenceNr)
          val resultSet = stmt.executeQuery()
          while (resultSet.next()) {
            val blob = resultSet.getBlob(1)
            serialization.deserialize(blob.getBytes(1, blob.length.toInt), classOf[PersistentRepr]) match {
              case Failure(e) =>
                stmt.close()
                throw e
              case Success(repr) =>
                recoveryCallback(repr)
            }
          }
          Try(stmt.close())
          ()
        }
        Try(conn.close())
        result
      }
    }
  }
}
