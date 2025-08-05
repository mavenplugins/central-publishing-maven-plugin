/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.client.httpclient.auth;

import static org.sonatype.central.publisher.client.PublisherConstants.HTTP_USERTOKEN_AUTH_SCHEME;

/**
 * An AuthProvider that will add the Authorization header to the request, using the UserToken AuthScheme as needed by
 * Central's UserToken authentication implementation
 */
public class UserTokenAuthProvider
    extends AbstractAuthProvider
{
  public UserTokenAuthProvider(
      final String organizationId,
      final String userId,
      final String nameCode,
      final String passCode)
  {
    super(organizationId, userId, nameCode, passCode);
  }

  @Override
  protected String getAuthScheme() {
    return HTTP_USERTOKEN_AUTH_SCHEME;
  }
}
