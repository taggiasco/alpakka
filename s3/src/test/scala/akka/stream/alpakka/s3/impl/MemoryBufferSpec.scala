/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.s3.impl

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.scaladsl.{Sink, Source}
import akka.testkit.TestKit
import akka.util.ByteString
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}
import org.scalatest.concurrent.ScalaFutures

class MemoryBufferSpec(_system: ActorSystem)
    extends TestKit(_system)
    with FlatSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ScalaFutures {

  def this() = this(ActorSystem("MemoryBufferSpec"))

  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(30, Millis))

  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system).withDebugLogging(true))

  "MemoryBuffer" should "emit a chunk on its output containg the concatenation of all input values" in {
    val result = Source(Vector(ByteString(1, 2, 3, 4, 5), ByteString(6, 7, 8, 9, 10, 11, 12), ByteString(13, 14)))
      .via(new MemoryBuffer(200))
      .runWith(Sink.seq)
      .futureValue

    result should have size (1)
    val chunk = result.head
    chunk.size should be(14)
    chunk.data.runWith(Sink.seq).futureValue should be(Seq(ByteString(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14)))
  }

  it should "fail if more than maxSize bytes are fed into it" in {
    whenReady(
      Source(Vector(ByteString(1, 2, 3, 4, 5), ByteString(6, 7, 8, 9, 10, 11, 12), ByteString(13, 14)))
        .via(new MemoryBuffer(10))
        .runWith(Sink.seq)
        .failed
    ) { e =>
      e shouldBe a[IllegalStateException]
    }
  }
}
