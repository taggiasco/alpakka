/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.file.javadsl;

import akka.NotUsed;
import akka.stream.javadsl.Source;
import akka.stream.javadsl.StreamConverters;
import scala.None;
import scala.None$;
import scala.collection.immutable.Nil$;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Directory {

  /**
   * List all files in the given directory
   */
  public static Source<Path, NotUsed> ls(Path directory) {
    return akka.stream.alpakka.file.scaladsl.Directory.ls(directory).asJava();
  }

  /**
   * Recursively list files and directories in the given directory, depth first.
   */
  public static Source<Path, NotUsed> walk(Path directory) {
    return StreamConverters.fromJavaStream(() -> Files.walk(directory));
  }

  /**
   * Recursively list files and directories in the given directory, depth first,
   * with a maximum directory depth limit and a possibly set of options (See {@link java.nio.file.Files#walk} for
   * details.
   */
  public static Source<Path, NotUsed> walk(Path directory, int maxDepth, FileVisitOption... options) {
    return StreamConverters.fromJavaStream(() -> Files.walk(directory, maxDepth, options));
  }
}
