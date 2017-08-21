/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.geode.internal.stage

import akka.stream.SourceShape
import akka.stream.stage.StageLogging
import org.apache.geode.cache.client.ClientCache
import org.apache.geode.cache.query.SelectResults

import scala.util.Try

abstract class GeodeQueryGraphLogic[V](val shape: SourceShape[V], val clientCache: ClientCache, val query: String)
    extends GeodeSourceStageLogic[V](shape, clientCache)
    with StageLogging {

  override def executeQuery() = Try {
    qs.newQuery(query)
      .execute()
      .asInstanceOf[SelectResults[V]]
      .iterator()
  }

}
