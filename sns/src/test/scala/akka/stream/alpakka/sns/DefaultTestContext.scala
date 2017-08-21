/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.sns

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import com.amazonaws.services.sns.AmazonSNSAsyncClient
import org.mockito.Mockito.reset
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}

import scala.concurrent.Await
import scala.concurrent.duration._

trait DefaultTestContext extends BeforeAndAfterAll with BeforeAndAfterEach with MockitoSugar { this: Suite =>

  implicit protected val system: ActorSystem = ActorSystem()
  implicit protected val mat: Materializer = ActorMaterializer()
  implicit protected val snsClient: AmazonSNSAsyncClient = mock[AmazonSNSAsyncClient]

  override protected def beforeEach(): Unit =
    reset(snsClient)

  override protected def afterAll(): Unit =
    Await.ready(system.terminate(), 5.seconds)

}
