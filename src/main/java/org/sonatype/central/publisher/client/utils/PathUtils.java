/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.client.utils;

import java.nio.file.FileSystems;
import java.nio.file.Path;

public class PathUtils
{
  private PathUtils() {
  }

  public static Path PathOf(final String first, final String... more) {
    return FileSystems.getDefault().getPath(first, more);
  }
}
