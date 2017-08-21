/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.amqp.javadsl

import java.util.concurrent.CompletionStage

import scala.compat.java8.FutureConverters._
import akka.Done
import akka.stream.alpakka.amqp._
import akka.util.ByteString

object AmqpSink {

  /**
   * Java API: Creates an [[AmqpSink]] that accepts [[OutgoingMessage]] elements.
   *
   * This stage materializes to a CompletionStage<Done>, which can be used to know when the Sink completes, either normally
   * or because of an amqp failure
   */
  def create(settings: AmqpSinkSettings): akka.stream.javadsl.Sink[OutgoingMessage, CompletionStage[Done]] =
    akka.stream.alpakka.amqp.scaladsl.AmqpSink(settings).mapMaterializedValue(f => f.toJava).asJava

  /**
   * Java API:
   *
   * Connects to an AMQP server upon materialization and sends incoming messages to the server.
   * Each materialized sink will create one connection to the broker. This stage sends messages to
   * the queue named in the replyTo options of the message instead of from settings declared at construction.
   *
   * This stage materializes to a CompletionStage<Done>, which can be used to know when the Sink completes, either normally
   * or because of an amqp failure
   */
  def createReplyTo(
      settings: AmqpReplyToSinkSettings
  ): akka.stream.javadsl.Sink[OutgoingMessage, CompletionStage[Done]] =
    akka.stream.alpakka.amqp.scaladsl.AmqpSink.replyTo(settings).mapMaterializedValue(f => f.toJava).asJava

  /**
   * Java API: Creates an [[AmqpSink]] that accepts ByteString elements.
   *
   * This stage materializes to a CompletionStage<Done>, which can be used to know when the Sink completes, either normally
   * or because of an amqp failure
   */
  def createSimple(settings: AmqpSinkSettings): akka.stream.javadsl.Sink[ByteString, CompletionStage[Done]] =
    akka.stream.alpakka.amqp.scaladsl.AmqpSink.simple(settings).mapMaterializedValue(f => f.toJava).asJava

}
