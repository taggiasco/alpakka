/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.xml.scaladsl

import akka.NotUsed
import akka.stream.alpakka.xml.ParseEvent
import akka.stream.alpakka.xml.Xml._
import akka.stream.scaladsl.Flow
import akka.util.ByteString

import scala.collection.immutable

object XmlParsing {

  /**
   * Parser Flow that takes a stream of ByteStrings and parses them to XML events similar to SAX.
   */
  val parser: Flow[ByteString, ParseEvent, NotUsed] =
    Flow.fromGraph(new StreamingXmlParser)

  /**
   * A Flow that transforms a stream of XML ParseEvents. This stage coalesces consequitive CData and Characters
   * events into a single Characters event or fails if the buffered string is larger than the maximum defined.
   */
  def coalesce(maximumTextLength: Int): Flow[ParseEvent, ParseEvent, NotUsed] =
    Flow.fromGraph(new Coalesce(maximumTextLength))

  /**
   * A Flow that transforms a stream of XML ParseEvents. This stage filters out any event not corresponding to
   * a certain path in the XML document. Any event that is under the specified path (including subpaths) is passed
   * through.
   */
  def subslice(path: immutable.Seq[String]): Flow[ParseEvent, ParseEvent, NotUsed] =
    Flow.fromGraph(new Subslice(path))
}
