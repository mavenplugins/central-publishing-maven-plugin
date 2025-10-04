/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class DirectoryUtils
{
  /**
   * Checks if the given directory contains any files.
   *
   * @param directory the directory to check
   * @return true if the directory contains files, false otherwise
   */
  public static boolean hasFiles(final File directory) {
    if (directory == null) {
      return false;
    }
    try {
      if (directory.exists()) {
        Path path = directory.toPath();
        if (Files.exists(path) && Files.isDirectory(path)) {
          try (Stream<Path> paths = Files.list(path)) {
            return paths.findFirst().isPresent();
          }
        }
      }
    }
    catch (IOException ignored) {
    }
    return false;
  }
}
