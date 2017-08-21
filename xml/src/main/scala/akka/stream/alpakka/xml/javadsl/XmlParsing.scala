/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.xml.javadsl

import akka.NotUsed
import akka.stream.alpakka.xml
import akka.stream.alpakka.xml.ParseEvent
import akka.util.ByteString

import scala.collection.JavaConverters._

object XmlParsing {

  /**
   * Parser Flow that takes a stream of ByteStrings and parses them to XML events similar to SAX.
   */
  def parser(): akka.stream.javadsl.Flow[ByteString, ParseEvent, NotUsed] =
    xml.scaladsl.XmlParsing.parser.asJava

  /**
   * A Flow that transforms a stream of XML ParseEvents. This stage coalesces consequitive CData and Characters
   * events into a single Characters event or fails if the buffered string is larger than the maximum defined.
   */
  def coalesce(maximumTextLength: Int): akka.stream.javadsl.Flow[ParseEvent, ParseEvent, NotUsed] =
    xml.scaladsl.XmlParsing.coalesce(maximumTextLength).asJava

  /**
   * A Flow that transforms a stream of XML ParseEvents. This stage filters out any event not corresponding to
   * a certain path in the XML document. Any event that is under the specified path (including subpaths) is passed
   * through.
   */
  def subslice(path: java.util.Collection[String]): akka.stream.javadsl.Flow[ParseEvent, ParseEvent, NotUsed] =
    xml.scaladsl.XmlParsing.subslice(path.asScala.map(identity)(collection.breakOut)).asJava
}
