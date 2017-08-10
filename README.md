# akka-persistence-jdbc

**WARNING**: This is a brand-new library that has not undergone any
performance, longevity, memory-leak, or robustness testing.  Use it at
your own risk.

This is an
[akka-persistence](http://doc.akka.io/docs/akka/current/scala/persistence.html)
plugin that uses JDBC to persist actor state to a database.

This plugin passes the `akka-persistence-tck` test suite.

## Features

* Minimal dependencies (uses JDBC directly).
* Uses the [HikariCP](https://github.com/brettwooldridge/HikariCP)
  connection pool.
* Makes use of standard battle-tested JVM JDBC drivers.
* Cross-compiles against Scala 2.11 and 2.12.

### Database Support

* MySQL
* MariaDB

## Building

Assuming you have sbt installed somewhere in your `PATH`, run

```
$ sbt +package
```

## Running Tests

The tests require a live MySQL (or MariaDB) instance to be running.  It
does not clean up after itself, so you'll have to manually drop tables
after running it (or between runs).  You'll also need to create a
database called `akka_persist_test`.  The default test setup assumes a
database instance running on localhost, with username `root` and no
password.  Just run:

```
$ sbt +test
```

## Usage

### Via Maven/SBT

This library is not yet published to a Maven repository, so you'll need
to build and publish it locally (`sbt +publishM2`) for now, and then:

#### Maven

```
<dependency>
  <groupId>org.spurint</groupId>
  <artifactId>akka-persistence-jdbc_${SCALA_BINARY_VERSION}</artifactId>
  <version>${VERSION}</version>
  <scope>compile</scope>
</dependency>
<dependency>
  <groupId>mysql</groupId>
  <artifactId>mysql-connector-java</artifactId>
  <version>${VERSION}</version>
  <scope>compile</scope>
</dependency>
```

#### SBT

```
libraryDependencies ++= Seq(
  "org.spurint" %% "akka-persistence-jdbc" % "VERSION" % Compile,
  "mysql" % "mysql-connector-java" % "VERSION" % Compile
)
```

### Application

Add to your app's `application.conf`:

```
akka.persistence.journal.plugin = "org.spurint.akka-persistence-jdbc.journal"
akka.persistence.snapshot-store.plugin = "org.spurint.akka-persistence-jdbc.snapshot-store"

org.spurint.akka-persistence-jdbc {
  journal {
    driver-class-name = "com.mysql.jdbc.Driver"
    jdbc-url = "jdbc:mysql://db.example.com/akka_persistence?user=akka&password=secret"
  }
  snapshot-store {
    driver-class-name = "com.mysql.jdbc.Driver"
    jdbc-url = "jdbc:mysql://db.example.com/akka_persistence?user=akka&password=secret"
  }
}
```

See the included `reference.conf` (in `src/main/resources`) for other
tweakable properties.

### Database

The plugin expects that the database you've specified already exists.
It will, however, create the needed tables for you on startup if they
don't exist already.

## Missing Features

* HikariCP exposes a bunch of configuration knobs that are not yet
  exposed.
* Lots of testing; I would not consider this production-quality yet.
* Support for RDBMSes other than MySQL/MariaDB.
