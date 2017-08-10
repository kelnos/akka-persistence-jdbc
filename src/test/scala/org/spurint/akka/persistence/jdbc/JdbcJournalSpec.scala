package org.spurint.akka.persistence.jdbc

import akka.persistence.CapabilityFlag
import akka.persistence.journal.JournalSpec
import com.typesafe.config.ConfigFactory

class JdbcJournalSpec extends JournalSpec(ConfigFactory.load()) {
  override protected def supportsRejectingNonSerializableObjects: CapabilityFlag = true
}
