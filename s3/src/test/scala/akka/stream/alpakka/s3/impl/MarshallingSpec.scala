/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.s3.impl

import java.time.Instant

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{MediaTypes, _}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.alpakka.s3.scaladsl.ListBucketResultContents
import akka.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpecLike, Matchers}

import scala.collection.immutable.Seq

class MarshallingSpec(_system: ActorSystem)
    extends TestKit(_system)
    with FlatSpecLike
    with Matchers
    with ScalaFutures {

  def this() = this(ActorSystem("MarshallingSpec"))

  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system).withDebugLogging(true))
  implicit val ec = materializer.executionContext

  val xmlString = """<?xml version="1.0" encoding="UTF-8"?>
                    |<ListBucketResult xmlns="http://s3.amazonaws.com/doc/2006-03-01/">
                    |    <Name>bucket</Name>
                    |    <Prefix/>
                    |    <KeyCount>205</KeyCount>
                    |    <MaxKeys>1000</MaxKeys>
                    |    <IsTruncated>false</IsTruncated>
                    |    <Contents>
                    |        <Key>my-image.jpg</Key>
                    |        <LastModified>2009-10-12T17:50:30.000Z</LastModified>
                    |        <ETag>&quot;fba9dede5f27731c9771645a39863328&quot;</ETag>
                    |        <Size>434234</Size>
                    |        <StorageClass>STANDARD</StorageClass>
                    |    </Contents>
                    |    <Contents>
                    |        <Key>my-image2.jpg</Key>
                    |        <LastModified>2009-10-12T17:50:31.000Z</LastModified>
                    |        <ETag>&quot;599bab3ed2c697f1d26842727561fd94&quot;</ETag>
                    |        <Size>1234</Size>
                    |        <StorageClass>REDUCED_REDUNDANCY</StorageClass>
                    |    </Contents>
                    |</ListBucketResult>""".stripMargin

  it should "initiate multipart upload when the region is us-east-1" in {
    val entity = HttpEntity(MediaTypes.`application/xml` withCharset HttpCharsets.`UTF-8`, xmlString)

    val result = Marshalling.listBucketResultUnmarshaller(entity)

    result.futureValue shouldEqual ListBucketResult(
      false,
      None,
      Seq(
        ListBucketResultContents("bucket",
                                 "my-image.jpg",
                                 "fba9dede5f27731c9771645a39863328",
                                 434234,
                                 Instant.parse("2009-10-12T17:50:30Z"),
                                 "STANDARD"),
        ListBucketResultContents("bucket",
                                 "my-image2.jpg",
                                 "599bab3ed2c697f1d26842727561fd94",
                                 1234,
                                 Instant.parse("2009-10-12T17:50:31Z"),
                                 "REDUCED_REDUNDANCY")
      )
    )
  }

}
