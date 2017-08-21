/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.azure.storagequeue.javadsl

import com.microsoft.azure.storage.queue.{CloudQueue, CloudQueueMessage}
import akka.stream.alpakka.azure.storagequeue.{AzureQueueSourceSettings, AzureQueueSourceStage}
import akka.stream.javadsl.Source
import akka.NotUsed
import java.util.function.Supplier

object AzureQueueSource {

  /**
   * Java API: creates a [[AzureQueueSource]] for a Azure CloudQueue.
   */
  def create(cloudQueue: Supplier[CloudQueue],
             settings: AzureQueueSourceSettings): Source[CloudQueueMessage, NotUsed] =
    Source.fromGraph(new AzureQueueSourceStage(() => cloudQueue.get(), settings))

  def create(cloudQueue: Supplier[CloudQueue]): Source[CloudQueueMessage, NotUsed] =
    create(cloudQueue, AzureQueueSourceSettings.Default)
}
