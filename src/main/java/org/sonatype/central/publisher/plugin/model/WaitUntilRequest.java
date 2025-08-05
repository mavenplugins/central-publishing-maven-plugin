/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.model;

import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Simple enum holding values on which WaitUntilRequest are requested to be used.
 */
public enum WaitUntilRequest
{
  UPLOADED,
  VALIDATED,
  PUBLISHED;

  public static boolean isValidValue(final String value) {
    try {
      valueOf(value.toUpperCase());
      return true;
    }
    catch (IllegalArgumentException ignore) {
      return false;
    }
  }

  public static List<String> toNames() {
    return Arrays.stream(WaitUntilRequest.values()).map(Enum::name).map(String::toLowerCase).collect(toList());
  }
}
