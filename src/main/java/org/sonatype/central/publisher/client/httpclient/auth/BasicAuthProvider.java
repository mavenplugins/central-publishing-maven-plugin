/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.client.httpclient.auth;

import static org.sonatype.central.publisher.client.PublisherConstants.HTTP_BASIC_AUTH_SCHEME;

/**
 * An AuthProvider that will add the Authorization header to the request, using the Basic AuthScheme as needed by
 * Central's Basic authentication implementation
 */
public class BasicAuthProvider
    extends AbstractAuthProvider
{
  public BasicAuthProvider(final String organizationId, final String username, final String password) {
    super(organizationId, username, username, password);
  }

  @Override
  protected String getAuthScheme() {
    return HTTP_BASIC_AUTH_SCHEME;
  }
}
