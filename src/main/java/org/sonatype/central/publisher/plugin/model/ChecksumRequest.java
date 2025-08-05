/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.model;

/**
 * Simple enum holding values on which Checksum are requested to be used.
 */
public enum ChecksumRequest
{
  ALL, // Will request MD5, SHA1, SHA256 and SHA512 to be generated
  REQUIRED, // Only MD5 and SHA1 will be requested to be generated
  NONE; // No Checksums will be requested to be generated.

  /**
   * Similar to {@link #valueOf(String)}, but allows case-insensitive and takes a default value to use if the given
   * {@code value} is invalid and doesn't match an enum.
   *
   * @param value        - value of which to get a {@link ChecksumRequest} for.
   * @param defaultValue - {@link ChecksumRequest} to use if the given {@code value} has no valid value.
   * @return ChecksumRequest
   */
  public static ChecksumRequest valueOf(final String value, final ChecksumRequest defaultValue) {
    try {
      return valueOf(value.toUpperCase());
    }
    catch (IllegalArgumentException ignore) {
      return defaultValue;
    }
  }
}
