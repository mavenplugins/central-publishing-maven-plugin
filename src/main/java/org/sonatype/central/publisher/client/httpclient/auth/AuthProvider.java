/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.client.httpclient.auth;

import java.util.Map;

public interface AuthProvider
{
  Map<String, String> getAuthHeaders();

  Map<String, String> getQueryParams();

  String getOrganizationId();

  String getUserId();

  String getPrincipal();

  String getCredential();
}
