/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.csv.scaladsl

import akka.NotUsed
import akka.stream.alpakka.csv.CsvParsingStage
import akka.stream.scaladsl.Flow
import akka.util.ByteString

object CsvParsing {

  val Backslash: Byte = '\\'
  val Comma: Byte = ','
  val SemiColon: Byte = ';'
  val Colon: Byte = ':'
  val Tab: Byte = '\t'
  val DoubleQuote: Byte = '"'

  /** Creates CSV parsing flow that reads CSV lines from incoming
   * [[akka.util.ByteString]] objects.
   */
  def lineScanner(delimiter: Byte = Comma,
                  quoteChar: Byte = DoubleQuote,
                  escapeChar: Byte = Backslash): Flow[ByteString, List[ByteString], NotUsed] =
    Flow.fromGraph(new CsvParsingStage(delimiter, quoteChar, escapeChar))
}
