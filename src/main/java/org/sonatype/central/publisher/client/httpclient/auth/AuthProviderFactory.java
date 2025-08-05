/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.client.httpclient.auth;

public class AuthProviderFactory
{
  public AuthProvider get(
      final AuthProviderType authProviderType,
      final String organizationId,
      final String userId,
      final String principal,
      final String credential)
  {
    if (authProviderType == null) {
      throw new IllegalArgumentException("Must provide a valid authProviderType for authentication!");
    }
    if (organizationId == null) {
      throw new IllegalArgumentException("Must provide a valid organizationId for authentication!");
    }
    if (userId == null) {
      throw new IllegalArgumentException("Must provide a valid userId for authentication!");
    }
    if (principal == null) {
      throw new IllegalArgumentException("Must provide a valid principal for authentication!");
    }
    if (credential == null) {
      throw new IllegalArgumentException("Must provide a valid credential for authentication!");
    }

    switch (authProviderType) {
      case BASIC:
        return new BasicAuthProvider(organizationId, principal, credential);
      case USERTOKEN:
        return new UserTokenAuthProvider(organizationId, userId, principal, credential);
    }

    throw new IllegalStateException(
        "Must provide a valid AuthProviderType {" + authProviderType.name() + "} for authentication!");
  }
}
