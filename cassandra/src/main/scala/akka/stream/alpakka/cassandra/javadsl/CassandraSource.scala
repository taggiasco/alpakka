/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.cassandra.javadsl

import java.util.concurrent.CompletableFuture

import akka.NotUsed
import akka.stream.alpakka.cassandra.CassandraSourceStage
import akka.stream.javadsl.Source
import com.datastax.driver.core.{Row, Session, Statement}

import scala.concurrent.Future

object CassandraSource {

  /**
   * Java API: creates a [[CassandraSource]] from a given statement.
   */
  def create(stmt: Statement, session: Session): Source[Row, NotUsed] =
    akka.stream.javadsl.Source.fromGraph(new CassandraSourceStage(Future.successful(stmt), session))

  /**
   * Java API: creates a [[CassandraSource]] from the result of a given CompletableFuture.
   */
  def createFromFuture(
      futStmt: CompletableFuture[Statement],
      session: Session
  ): Source[Row, NotUsed] = {
    import scala.compat.java8.FutureConverters._
    akka.stream.javadsl.Source.fromGraph(new CassandraSourceStage(futStmt.toScala, session))
  }
}
