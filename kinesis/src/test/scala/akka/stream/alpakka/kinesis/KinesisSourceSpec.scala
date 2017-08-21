/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.kinesis

import java.util
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

import akka.stream.alpakka.kinesis.KinesisSourceErrors.NoShardsError
import akka.stream.alpakka.kinesis.scaladsl.KinesisSource
import akka.stream.testkit.scaladsl.TestSink
import akka.util.ByteString
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.kinesis.model._
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.duration._

class KinesisSourceSpec extends WordSpecLike with Matchers with DefaultTestContext {

  implicit class recordToString(r: Record) {
    def utf8String: String = ByteString(r.getData).utf8String
  }

  "KinesisSource" must {

    val shardSettings =
      ShardSettings("stream_name", "shard-id", ShardIteratorType.TRIM_HORIZON, None, None, 1.second, 500)

    "poll for records" in new KinesisSpecContext with WithGetShardIteratorSuccess with WithGetRecordsSuccess {
      override def shards: util.List[Shard] = util.Arrays.asList(new Shard().withShardId("id"))

      override def records = util.Arrays.asList(
        new Record().withData(ByteString("1").toByteBuffer),
        new Record().withData(ByteString("2").toByteBuffer)
      )

      val probe = KinesisSource.basic(shardSettings, amazonKinesisAsync).runWith(TestSink.probe)

      probe.requestNext.utf8String shouldEqual "1"
      probe.requestNext.utf8String shouldEqual "2"
      probe.requestNext.utf8String shouldEqual "1"
      probe.requestNext.utf8String shouldEqual "2"
    }

    "poll for records with mutliple requests" in new KinesisSpecContext with WithGetShardIteratorSuccess
    with WithGetRecordsSuccess {
      override def shards: util.List[Shard] = util.Arrays.asList(new Shard().withShardId("id"))

      override def records = util.Arrays.asList(
        new Record().withData(ByteString("1").toByteBuffer),
        new Record().withData(ByteString("2").toByteBuffer)
      )

      val probe = KinesisSource.basic(shardSettings, amazonKinesisAsync).runWith(TestSink.probe)

      probe.request(2)
      probe.expectNext().utf8String shouldEqual "1"
      probe.expectNext().utf8String shouldEqual "2"
      probe.expectNoMsg()
    }

    "wait for request before passing downstream" in new KinesisSpecContext with WithGetShardIteratorSuccess
    with WithGetRecordsSuccess {
      override def shards: util.List[Shard] = util.Arrays.asList(new Shard().withShardId("id"))

      override def records = util.Arrays.asList(
        new Record().withData(ByteString("1").toByteBuffer),
        new Record().withData(ByteString("2").toByteBuffer),
        new Record().withData(ByteString("3").toByteBuffer),
        new Record().withData(ByteString("4").toByteBuffer),
        new Record().withData(ByteString("5").toByteBuffer),
        new Record().withData(ByteString("6").toByteBuffer)
      )

      val probe = KinesisSource.basic(shardSettings, amazonKinesisAsync).runWith(TestSink.probe)

      probe.request(1)
      probe.expectNext().utf8String shouldEqual "1"
      probe.expectNoMsg
      probe.requestNext().utf8String shouldEqual "2"
      probe.requestNext().utf8String shouldEqual "3"
      probe.requestNext().utf8String shouldEqual "4"
      probe.requestNext().utf8String shouldEqual "5"
      probe.requestNext().utf8String shouldEqual "6"
      probe.requestNext().utf8String shouldEqual "1"

    }

    "merge multiple shards" in new KinesisSpecContext with WithGetShardIteratorSuccess with WithGetRecordsSuccess {
      val mergeSettings = List(
        shardSettings.copy(shardId = "0"),
        shardSettings.copy(shardId = "1")
      )

      override def shards: util.List[Shard] =
        util.Arrays.asList(new Shard().withShardId("1"), new Shard().withShardId("2"))

      override def records = util.Arrays.asList(
        new Record().withData(ByteString("1").toByteBuffer),
        new Record().withData(ByteString("2").toByteBuffer),
        new Record().withData(ByteString("3").toByteBuffer)
      )

      val probe = KinesisSource.basicMerge(mergeSettings, amazonKinesisAsync).map(_.utf8String).runWith(TestSink.probe)

      probe.request(6)
      probe.expectNextUnordered("1", "1", "2", "2", "3", "3")
    }

    "complete stage when next shard iterator is null" in new KinesisSpecContext with WithGetShardIteratorSuccess
    with WithGetRecordsSuccess {
      override def records = util.Arrays.asList(new Record().withData(ByteString("1").toByteBuffer))

      val probe = KinesisSource.basic(shardSettings, amazonKinesisAsync).runWith(TestSink.probe)

      probe.requestNext.utf8String shouldEqual "1"
      nextShardIterator.set(null)
      probe.request(1)
      probe.expectNext()
      probe.expectComplete()
    }

    "fail with error when GetStreamRequest fails" in new KinesisSpecContext with WithGetShardIteratorSuccess
    with WithGetRecordsFailure {
      val probe = KinesisSource.basic(shardSettings, amazonKinesisAsync).runWith(TestSink.probe)
      probe.request(1)
      probe.expectError() shouldBe an[KinesisSourceErrors.GetRecordsError.type]
    }
  }

  trait KinesisSpecContext {
    def shards: util.List[Shard] = util.Arrays.asList(new Shard().withShardId("id"))

    def shardIterator: String = "iterator"

    def records: util.List[Record] = new util.ArrayList()

    val nextShardIterator = new AtomicReference[String]("next")

    val describeStreamResult = mock[DescribeStreamResult]
    val streamDescription = mock[StreamDescription]

    when(amazonKinesisAsync.describeStream(anyString())).thenReturn(describeStreamResult)
    when(describeStreamResult.getStreamDescription).thenReturn(streamDescription)
    when(streamDescription.getShards).thenReturn(shards)
    when(streamDescription.getHasMoreShards).thenReturn(false)

    val getShardIteratorRequest = new GetShardIteratorRequest
    val getShardIteratorResult = new GetShardIteratorResult().withShardIterator(shardIterator)
    val getRecordsRequest = new GetRecordsRequest

    def getRecordsResult = new GetRecordsResult().withRecords(records).withNextShardIterator(nextShardIterator.get())

  }

  trait WithGetShardIteratorSuccess { self: KinesisSpecContext =>
    when(amazonKinesisAsync.getShardIteratorAsync(any(), any())).thenAnswer(new Answer[AnyRef] {
      override def answer(invocation: InvocationOnMock): AnyRef = {
        invocation
          .getArgument[AsyncHandler[GetShardIteratorRequest, GetShardIteratorResult]](1)
          .onSuccess(getShardIteratorRequest, getShardIteratorResult)
        CompletableFuture.completedFuture(getShardIteratorResult)
      }
    })
  }

  trait WithGetShardIteratorFailure { self: KinesisSpecContext =>
    when(amazonKinesisAsync.getShardIteratorAsync(any(), any())).thenAnswer(new Answer[AnyRef] {
      override def answer(invocation: InvocationOnMock): AnyRef = {
        invocation
          .getArgument[AsyncHandler[GetShardIteratorRequest, GetShardIteratorResult]](1)
          .onError(new Exception("fail"))
        CompletableFuture.completedFuture(getShardIteratorResult)
      }
    })
  }

  trait WithGetRecordsSuccess { self: KinesisSpecContext =>
    when(amazonKinesisAsync.getRecordsAsync(any(), any())).thenAnswer(new Answer[AnyRef] {
      override def answer(invocation: InvocationOnMock) = {
        invocation
          .getArgument[AsyncHandler[GetRecordsRequest, GetRecordsResult]](1)
          .onSuccess(getRecordsRequest, getRecordsResult)
        CompletableFuture.completedFuture(getRecordsResult)
      }
    })
  }

  trait WithGetRecordsFailure { self: KinesisSpecContext =>
    when(amazonKinesisAsync.getRecordsAsync(any(), any())).thenAnswer(new Answer[AnyRef] {
      override def answer(invocation: InvocationOnMock) = {
        invocation
          .getArgument[AsyncHandler[GetRecordsRequest, GetRecordsResult]](1)
          .onError(new Exception("fail"))
        CompletableFuture.completedFuture(getRecordsResult)
      }
    })
  }

}
