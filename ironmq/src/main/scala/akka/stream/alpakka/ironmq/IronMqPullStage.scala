/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.ironmq

import akka.Done
import akka.stream.stage._
import akka.stream._

import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success}
import IronMqSettings.ConsumerSettings
import akka.stream.alpakka.ironmq.scaladsl.CommittableMessage

object IronMqPullStage {

  private val FetchMessagesTimerKey = "fetch-messages"
}

/**
 * This stage will fetch messages from IronMq and buffer them internally.
 *
 * It is implemented as a timed loop, each invocation will fetch new messages from IronMq if the amount of buffered
 * messages is lower than [[ConsumerSettings.bufferMinSize]].
 *
 * The frequency of the loop is controlled by [[ConsumerSettings.fetchInterval]] while the amount of time the client is
 * blocked on the HTTP request waiting for messages is controlled by [[ConsumerSettings.pollTimeout]].
 *
 * Keep in mind that the IronMq time unit is the second, so any value below the second is considered 0.
 */
class IronMqPullStage(queue: Queue.Name, settings: IronMqSettings)
    extends GraphStage[SourceShape[CommittableMessage]] {

  import IronMqPullStage._

  val consumerSettings: ConsumerSettings = settings.consumerSettings
  import consumerSettings._

  private val out: Outlet[CommittableMessage] = Outlet("IronMqPull.out")

  override protected val initialAttributes: Attributes = Attributes.name("IronMqPull")

  override val shape: SourceShape[CommittableMessage] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): TimerGraphStageLogic =
    new TimerGraphStageLogic(shape) {

      implicit def ec: ExecutionContextExecutor = materializer.executionContext

      // This flag avoid run concurrent fetch from IronMQ
      private var fetching: Boolean = false

      private var buffer: List[ReservedMessage] = List.empty
      private var client: IronMqClient = _ // set in preStart

      override def preStart(): Unit = {
        super.preStart()
        client = IronMqClient(settings)(ActorMaterializerHelper.downcast(materializer).system, materializer)
      }

      override def postStop(): Unit =
        super.postStop()

      setHandler(
        out,
        new OutHandler {

          override def onPull(): Unit = {

            if (!isTimerActive(FetchMessagesTimerKey)) {
              schedulePeriodically(FetchMessagesTimerKey, fetchInterval)
            }

            deliveryMessages()
          }
        }
      )

      override protected def onTimer(timerKey: Any): Unit = timerKey match {

        case FetchMessagesTimerKey =>
          fetchMessages()

      }

      def fetchMessages(): Unit =
        if (!fetching && buffer.size < bufferMinSize) {

          fetching = true

          client
            .reserveMessages(
              queue,
              bufferMaxSize - buffer.size,
              watch = pollTimeout,
              timeout = reservationTimeout
            )
            .onComplete {
              case Success(xs) =>
                updateBuffer.invoke(xs.toList)
                updateFetching.invoke(false)
              case Failure(error) =>
                fail(out, error)
                updateFetching.invoke(false)
            }
        }

      def deliveryMessages(): Unit =
        while (buffer.nonEmpty && isAvailable(out)) {
          val messageToDelivery: ReservedMessage = buffer.head

          val committableMessage = new CommittableMessage {
            override val message =
              messageToDelivery.message
            override def commit() =
              client.deleteMessages(queue, messageToDelivery.reservation).map(_ => Done)
          }

          push(out, committableMessage)
          buffer = buffer.tail
        }

      private val updateBuffer = getAsyncCallback { xs: List[ReservedMessage] =>
        buffer = buffer ::: xs
        deliveryMessages()
      }

      private val updateFetching = getAsyncCallback { x: Boolean =>
        fetching = x
      }
    }

}
