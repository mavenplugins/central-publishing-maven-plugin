/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.client.httpclient.auth;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static org.sonatype.central.publisher.client.PublisherConstants.HTTP_AUTHORIZATION_HEADER;
import static org.sonatype.central.publisher.client.PublisherConstants.ORGANIZATION_ID_QUERY_PARAM;
import static org.sonatype.central.publisher.client.PublisherConstants.USER_ID_QUERY_PARAM;

public abstract class AbstractAuthProvider
    implements AuthProvider
{
  private final Map<String, String> headers = new HashMap<>();

  private final Map<String, String> queryParams = new HashMap<>();

  private final String organizationId;

  private final String userId;

  private final String principal;

  private final String credential;

  protected AbstractAuthProvider(
      final String organizationId,
      final String userId,
      final String principal,
      final String credential)
  {
    headers.put(HTTP_AUTHORIZATION_HEADER, buildAuthorizationHeaderValue(principal, credential));
    queryParams.put(ORGANIZATION_ID_QUERY_PARAM, organizationId);
    queryParams.put(USER_ID_QUERY_PARAM, userId);
    this.organizationId = organizationId;
    this.userId = userId;
    this.principal = principal;
    this.credential = credential;
  }

  @Override
  public Map<String, String> getAuthHeaders() {
    return new HashMap<>(headers);
  }

  @Override
  public Map<String, String> getQueryParams() {
    return queryParams;
  }

  @Override
  public String getOrganizationId() {
    return organizationId;
  }

  @Override
  public String getUserId() {
    return userId;
  }

  @Override
  public String getPrincipal() {
    return principal;
  }

  @Override
  public String getCredential() {
    return credential;
  }

  private String buildAuthorizationHeaderValue(String principal, String credential) {
    String value = principal + ":" + credential;
    return getAuthScheme() + " " + Base64.getEncoder().encodeToString(value.getBytes());
  }

  protected abstract String getAuthScheme();
}
