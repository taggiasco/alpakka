/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.file.javadsl;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.KillSwitches;
import akka.stream.Materializer;
import akka.stream.UniqueKillSwitch;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.testkit.TestSubscriber;
import akka.testkit.TestKit;
import akka.util.ByteString;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import scala.concurrent.duration.FiniteDuration;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.Assert.assertEquals;


public class FileTailSourceTest {

  private FileSystem fs;
  private ActorSystem system;
  private Materializer materializer;

  @Before
  public void setup() {
    fs = Jimfs.newFileSystem(Configuration.unix());
    system = ActorSystem.create();
    materializer = ActorMaterializer.create(system);
  }



  @Test
  public void canReadAnEntireFile() throws Exception {
    final Path path = fs.getPath("/file");
    final String dataInFile = "a\nb\nc\nd";
    Files.write(path, dataInFile.getBytes(UTF_8));

    final Source<ByteString, NotUsed> source = akka.stream.alpakka.file.javadsl.FileTailSource.create(
      path,
      8192, // chunk size
      0, // starting position
      FiniteDuration.create(250, TimeUnit.MILLISECONDS));

    final TestSubscriber.Probe<ByteString> subscriber = TestSubscriber.probe(system);

    final UniqueKillSwitch killSwitch =
      source.viaMat(KillSwitches.single(), Keep.right())
        .to(Sink.fromSubscriber(subscriber))
        .run(materializer);

    ByteString result = subscriber.requestNext();
    assertEquals(dataInFile, result.utf8String());

    killSwitch.shutdown();
    subscriber.expectComplete();

  }

  @Test
  public void willReadNewLinesAppendedAfterReadingTheInitialContents() throws Exception {
    final Path path = fs.getPath("/file");
    Files.write(path, "a\n".getBytes(UTF_8));

    final Source<String, NotUsed> source = akka.stream.alpakka.file.javadsl.FileTailSource.createLines(
      path,
      8192, // chunk size
      FiniteDuration.create(250, TimeUnit.MILLISECONDS),
      "\n",
      StandardCharsets.UTF_8
    );

    final TestSubscriber.Probe<String> subscriber = TestSubscriber.probe(system);

    final UniqueKillSwitch killSwitch =
      source.viaMat(KillSwitches.single(), Keep.right())
        .to(Sink.fromSubscriber(subscriber))
        .run(materializer);

    String result1 = subscriber.requestNext();
    assertEquals("a", result1);

    subscriber.request(1);
    Files.write(path, "b\n".getBytes(UTF_8), WRITE, APPEND);
    assertEquals("b", subscriber.expectNext());

    Files.write(path, "c\n".getBytes(UTF_8), WRITE, APPEND);
    subscriber.request(1);
    assertEquals("c", subscriber.expectNext());

    killSwitch.shutdown();
    subscriber.expectComplete();
  }

  @After
  public void tearDown() throws Exception {
    fs.close();
    fs = null;
    TestKit.shutdownActorSystem(system, FiniteDuration.create(10, TimeUnit.SECONDS), true);
    system = null;
    materializer = null;
  }


  // small sample of usage, tails the first argument file path
  public static void main(String... args) {
    if(args.length != 1) throw new IllegalArgumentException("Usage: FileTailSourceTest [path]");
    final String path = args[0];

    final ActorSystem system = ActorSystem.create();
    final Materializer materializer = ActorMaterializer.create(system);

    // #simple-lines
    final FileSystem fs = FileSystems.getDefault();
    final FiniteDuration pollingInterval = FiniteDuration.create(250, TimeUnit.MILLISECONDS);
    final int maxLineSize = 8192;

    final Source<String, NotUsed> lines =
      akka.stream.alpakka.file.javadsl.FileTailSource.createLines(fs.getPath(path), maxLineSize, pollingInterval);

    lines.runForeach((line) -> System.out.println(line), materializer);
    // #simple-lines
  }

}
