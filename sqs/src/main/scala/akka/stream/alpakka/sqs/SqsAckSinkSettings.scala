/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.sqs

object SqsAckSinkSettings {
  val Defaults = SqsAckSinkSettings(maxInFlight = 10)
}

//#SqsAckSinkSettings
final case class SqsAckSinkSettings(maxInFlight: Int) {
  require(maxInFlight > 0)
}
//#SqsAckSinkSettings

sealed trait MessageAction
final case class Delete() extends MessageAction
final case class Ignore() extends MessageAction
final case class ChangeMessageVisibility(visibilityTimeout: Int) extends MessageAction {
  // SQS requirements
  require(0 <= visibilityTimeout && visibilityTimeout <= 43200)
}
