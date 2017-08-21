/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.dynamodb.scaladsl

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.alpakka.dynamodb.AwsOp
import akka.stream.alpakka.dynamodb.impl.{DynamoClientImpl, DynamoSettings}
import akka.stream.scaladsl.{Sink, Source}

object DynamoClient {
  def apply(settings: DynamoSettings)(implicit system: ActorSystem, materializer: Materializer) =
    new DynamoClient(settings)
}

final class DynamoClient(settings: DynamoSettings)(implicit system: ActorSystem, materializer: Materializer) {
  private val client = new DynamoClientImpl(settings, DynamoImplicits.errorResponseHandler)

  val flow = client.flow
  def single(op: AwsOp) = Source.single(op).via(client.flow).map(_.asInstanceOf[op.B]).runWith(Sink.head)
}
