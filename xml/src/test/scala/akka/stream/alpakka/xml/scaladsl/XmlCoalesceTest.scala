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

class XmlCoalesceTest extends WordSpec with Matchers with BeforeAndAfterAll {
  implicit val system = ActorSystem("Test")
  implicit val mat = ActorMaterializer()

  val parse = Flow[String]
    .map(ByteString(_))
    .via(XmlParsing.parser)
    .via(XmlParsing.coalesce(10))
    .toMat(Sink.seq)(Keep.right)

  "XML coalesce support" must {

    "properly unify a chain of character chunks" in {
      val docStream =
        Source
          .single("<doc>")
          .concat(Source((0 to 9).map(_.toString)))
          .concat(Source.single("</doc>"))

      val result = Await.result(docStream.runWith(parse), 3.seconds)
      result should ===(
        List(
          StartDocument,
          StartElement("doc", Map.empty),
          Characters("0123456789"),
          EndElement("doc"),
          EndDocument
        )
      )
    }

    "properly unify a chain of CDATA chunks" in {
      val docStream =
        Source
          .single("<doc>")
          .concat(Source((0 to 9).map(i => s"<![CDATA[$i]]>")))
          .concat(Source.single("</doc>"))

      val result = Await.result(docStream.runWith(parse), 3.seconds)
      result should ===(
        List(
          StartDocument,
          StartElement("doc", Map.empty),
          Characters("0123456789"),
          EndElement("doc"),
          EndDocument
        )
      )
    }

    "properly unify a chain of CDATA and character chunks" in {
      val docStream =
        Source
          .single("<doc>")
          .concat(Source((0 to 9).map { i =>
            if (i % 2 == 0) s"<![CDATA[$i]]>"
            else i.toString
          }))
          .concat(Source.single("</doc>"))

      val result = Await.result(docStream.runWith(parse), 3.seconds)
      result should ===(
        List(
          StartDocument,
          StartElement("doc", Map.empty),
          Characters("0123456789"),
          EndElement("doc"),
          EndDocument
        )
      )
    }

    "properly report an error if text limit is exceeded" in {
      val docStream =
        Source
          .single("<doc>")
          .concat(Source((0 to 10).map(_.toString)))
          .concat(Source.single("</doc>"))

      an[IllegalStateException] shouldBe thrownBy(Await.result(docStream.runWith(parse), 3.seconds))
    }

  }

  override protected def afterAll(): Unit = system.terminate()
}
