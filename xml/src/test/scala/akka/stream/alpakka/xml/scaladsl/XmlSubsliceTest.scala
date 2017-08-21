/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.xml.scaladsl

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.alpakka.xml._
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.util.ByteString
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.concurrent.Await
import scala.concurrent.duration._

class XmlSubsliceTest extends WordSpec with Matchers with BeforeAndAfterAll {
  implicit val system = ActorSystem("Test")
  implicit val mat = ActorMaterializer()

  //#subslice
  val parse = Flow[String]
    .map(ByteString(_))
    .via(XmlParsing.parser)
    .via(XmlParsing.subslice("doc" :: "elem" :: "item" :: Nil))
    .toMat(Sink.seq)(Keep.right)
  //#subslice

  "XML subslice support" must {

    "properly extract subslices of events" in {
      val doc =
        """
          |<doc>
          |  <elem>
          |    <item>i1</item>
          |    <item>i2</item>
          |    <item>i3</item>
          |  </elem>
          |</doc>
        """.stripMargin

      val result = Await.result(Source.single(doc).runWith(parse), 3.seconds)
      result should ===(
        List(
          Characters("i1"),
          Characters("i2"),
          Characters("i3")
        )
      )
    }

    "properly extract subslices of nested events" in {

      //#subslice-usage
      val doc =
        """
          |<doc>
          |  <elem>
          |    <item>i1</item>
          |    <item><sub>i2</sub></item>
          |    <item>i3</item>
          |  </elem>
          |</doc>
        """.stripMargin
      val resultFuture = Source.single(doc).runWith(parse)
      //#subslice-usage

      val result = Await.result(resultFuture, 3.seconds)
      result should ===(
        List(
          Characters("i1"),
          StartElement("sub", Map.empty),
          Characters("i2"),
          EndElement("sub"),
          Characters("i3")
        )
      )
    }

    "properly ignore matches not deep enough" in {
      val doc =
        """
          |<doc>
          |  <elem>
          |     I am lonely here :(
          |  </elem>
          |</doc>
        """.stripMargin

      val result = Await.result(Source.single(doc).runWith(parse), 3.seconds)
      result should ===(Nil)
    }

    "properly ignore partial matches" in {
      val doc =
        """
          |<doc>
          |  <elem>
          |     <notanitem>ignore me</notanitem>
          |     <notanitem>ignore me</notanitem>
          |     <foo>ignore me</foo>
          |  </elem>
          |  <bar></bar>
          |</doc>
        """.stripMargin

      val result = Await.result(Source.single(doc).runWith(parse), 3.seconds)
      result should ===(Nil)
    }

    "properly filter from the combination of the above" in {
      val doc =
        """
          |<doc>
          |  <elem>
          |    <notanitem>ignore me</notanitem>
          |    <notanitem>ignore me</notanitem>
          |    <foo>ignore me</foo>
          |    <item>i1</item>
          |    <item><sub>i2</sub></item>
          |    <item>i3</item>
          |  </elem>
          |  <elem>
          |    not me please
          |  </elem>
          |  <elem><item>i4</item></elem>
          |</doc>
        """.stripMargin

      val result = Await.result(Source.single(doc).runWith(parse), 3.seconds)
      result should ===(
        List(
          Characters("i1"),
          StartElement("sub", Map.empty),
          Characters("i2"),
          EndElement("sub"),
          Characters("i3"),
          Characters("i4")
        )
      )
    }

  }

  override protected def afterAll(): Unit = system.terminate()
}
