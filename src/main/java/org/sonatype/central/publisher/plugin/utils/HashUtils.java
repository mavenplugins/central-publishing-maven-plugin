/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.utils;

import java.io.File;

import javax.annotation.Nullable;

public interface HashUtils
{
  /**
   * Get the hash for a given file.
   *
   * @param file      - {@link File}
   * @param algorithm - {@link HashAlgorithm}
   * @return String or null if the file didn't exist or issues occurred reading it.
   */
  @Nullable
  String hash(final File file, final HashAlgorithm algorithm);

  /**
   * Creates a checksum file for a given file. The hash file will be created alongside the given {@code file} in the same
   * parent directory. Example the the file /test/1.0.jar, after calling this method with, for example
   * {@link HashAlgorithm#SHA512}, have alongside itself the file /test/1.0.jar.sha512
   *
   * @param file - {@link File}
   * @param algorithm - {@link HashAlgorithm}
   * @return File - the {@link File} with the hash for the given {@code file}, or null if unable to create it.
   */
  @Nullable
  File createChecksumFile(final File file, final HashAlgorithm algorithm);

  /**
   * Test whether a given {@link File} is a file that is considered a checksum file based on its extension ending
   * with {@link HashAlgorithm#MD5}, {@link HashAlgorithm#SHA1},{@link HashAlgorithm#SHA256} or
   * {@link HashAlgorithm#SHA512}.
   *
   * @param file - {@link File}
   * @return true if the given file is considered a checksum file.
   */
  boolean isChecksumFile(final File file);

  /**
   * Test whether a given {@link File} is a file that is considered a signature file with .asc extension
    * @param file - {@link File}
   * @return true if the given file is considered a signature file
   */
  boolean isSignatureFile(File file);
}
