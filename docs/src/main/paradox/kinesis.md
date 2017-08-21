# AWS Kinesis Connector

The AWS Kinesis connector provides an Akka Stream Source for consuming Kinesis Stream records.

For more information about Kinesis please visit the [official documentation](https://aws.amazon.com/documentation/kinesis/).

## Artifacts

sbt
:   @@@vars
    ```scala
    libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-kinesis" % "$version$"
    ```
    @@@

Maven
:   @@@vars
    ```xml
    <dependency>
      <groupId>com.lightbend.akka</groupId>
      <artifactId>akka-stream-alpakka-kinesis$scalaBinaryVersion$</artifactId>
      <version>$version$</version>
    </dependency>
    ```
    @@@

Gradle
:   @@@vars
    ```gradle
    dependencies {
      compile group: "com.lightbend.akka", name: "akka-stream-alpakka-kinesis$scalaBinaryVersion$", version: "$version$"
    }
    ```
    @@@

## Usage

Sources provided by this connector need a `AmazonKinesisAsync` instance to consume messages from a shard.

@@@ note
The `AmazonKinesisAsync` instance you supply is thread-safe and can be shared amongst multiple `GraphStages`. As a result, individual `GraphStages` will not automatically shutdown the supplied client when they complete.
@@@

Scala
: @@snip (../../../../kinesis/src/test/scala/akka/stream/alpakka/kinesis/scaladsl/Examples.scala) { #init-client }

Java
: @@snip (../../../../kinesis/src/test/java/akka/stream/alpakka/kinesis/javadsl/Examples.java) { #init-client }

We will also need an @scaladoc[ActorSystem](akka.actor.ActorSystem) and an @scaladoc[ActorMaterializer](akka.stream.ActorMaterializer).

Scala
: @@snip (../../../../kinesis/src/test/scala/akka/stream/alpakka/kinesis/scaladsl/Examples.scala) { #init-system }

Java
: @@snip (../../../../kinesis/src/test/java/akka/stream/alpakka/kinesis/javadsl/Examples.java) { #init-system }

### Using the Source

The `KinesisSource` creates one `GraphStage` per shard. Reading from a shard requires an instance of `ShardSettings`. 

Scala
: @@snip (../../../../kinesis/src/test/scala/akka/stream/alpakka/kinesis/scaladsl/Examples.scala) { #settings }

Java
: @@snip (../../../../kinesis/src/test/java/akka/stream/alpakka/kinesis/javadsl/Examples.java) { #settings }

You have the choice of reading from a single shard, or reading from multiple shards. In the case of multiple shards the results of running a separate `GraphStage` for each shard will be merged together. 

@@@ warning
The `GraphStage` associated with a shard will remain open until the graph is stopped, or a [GetRecords](http://docs.aws.amazon.com/kinesis/latest/APIReference/API_GetRecords.html) result returns an empty shard iterator indicating that the shard has been closed. This means that if you wish to continue processing records after a merge or reshard, you will need to recreate the source with the results of a new [DescribeStream](http://docs.aws.amazon.com/kinesis/latest/APIReference/API_DescribeStream.html) request, which can be done by simply creating a new `KinesisSource`. You can read more about adapting to a reshard [here](http://docs.aws.amazon.com/streams/latest/dev/developing-consumers-with-sdk.html).
@@@

For a single shard you simply provide the settings for a single shard.
 
Scala
: @@snip (../../../../kinesis/src/test/scala/akka/stream/alpakka/kinesis/scaladsl/Examples.scala) { #single }

Java
: @@snip (../../../../kinesis/src/test/java/akka/stream/alpakka/kinesis/javadsl/Examples.java) { #single }

You can merge multiple shards by providing a list settings.
 
Scala
: @@snip (../../../../kinesis/src/test/scala/akka/stream/alpakka/kinesis/scaladsl/Examples.scala) { #list }

Java
: @@snip (../../../../kinesis/src/test/java/akka/stream/alpakka/kinesis/javadsl/Examples.java) { #list }

The constructed `Source` will return [Record](http://docs.aws.amazon.com/kinesis/latest/APIReference/API_Record.html)
objects by calling [GetRecords](http://docs.aws.amazon.com/kinesis/latest/APIReference/API_GetRecords.html) at the specified interval and according to the downstream demand. 


