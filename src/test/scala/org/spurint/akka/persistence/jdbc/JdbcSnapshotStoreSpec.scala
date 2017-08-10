package org.spurint.akka.persistence.jdbc

import akka.persistence.snapshot.SnapshotStoreSpec
import com.typesafe.config.ConfigFactory

class JdbcSnapshotStoreSpec extends SnapshotStoreSpec(ConfigFactory.load()) {

}
