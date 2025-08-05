/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin.utils;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Hash Algorithm wrapper around {@link Hashing} for easy usage, allowing for creating of default
 * {@link HashAlgorithm}s and easy reuse.
 */
public class HashAlgorithm
{
  public static final String MD5_NAME = "md5";

  public static final String SHA1_NAME = "sha1";

  public static final String SHA256_NAME = "sha256";

  public static final String SHA512_NAME = "sha512";

  public static final HashAlgorithm MD5 = new HashAlgorithm(MD5_NAME, Hashing.md5());

  public static final HashAlgorithm SHA1 = new HashAlgorithm(SHA1_NAME, Hashing.sha1());

  public static final HashAlgorithm SHA256 = new HashAlgorithm(SHA256_NAME, Hashing.sha256());

  public static final HashAlgorithm SHA512 = new HashAlgorithm(SHA512_NAME, Hashing.sha512());

  private final String name;

  private final HashFunction function;

  public HashAlgorithm(final String name, final HashFunction function) {
    this.name = checkNotNull(name);
    this.function = checkNotNull(function);
  }

  public String name() {
    return name;
  }

  public HashFunction function() {
    return function;
  }
}
