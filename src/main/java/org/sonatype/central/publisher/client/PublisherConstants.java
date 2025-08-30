/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.client;

public class PublisherConstants
{
  public static final String DEFAULT_CENTRAL_BASEURL = "https://central.sonatype.com";

  public static final String STATUS_ENDPOINT_URL = "/api/v1/publisher/status";

  public static final String UPLOAD_ENDPOINT_URL = "/api/v1/publisher/upload";

  public static final String PUBLISHED_ENDPOINT_URL = "/api/v1/publisher/published";

  public static final String DELETE_ENDPOINT_URL = "/api/v1/publisher/deployment/%s";

  public static final String HTTP_AUTHORIZATION_HEADER = "Authorization";

  public static final String HTTP_USERTOKEN_AUTH_SCHEME = "UserToken";

  public static final String ORGANIZATION_ID_QUERY_PARAM = "orgId";

  public static final String USER_ID_QUERY_PARAM = "userId";

  public static final String DEPLOYMENT_NAME_QUERY_PARAM = "name";

  public static final String DEPLOYMENT_ID_QUERY_PARAM = "id";

  public static final String DEPLOYMENT_PUBLISHING_TYPE_QUERY_PARAM = "publishingType";

  public static final String DEFAULT_ORGANIZATION_ID = "org";

  public static final String COMPONENT_NAMESPACE_QUERY_PARAM = "namespace";

  public static final String COMPONENT_NAME_QUERY_PARAM = "name";

  public static final String COMPONENT_VERSION_QUERY_PARAM = "version";
}
