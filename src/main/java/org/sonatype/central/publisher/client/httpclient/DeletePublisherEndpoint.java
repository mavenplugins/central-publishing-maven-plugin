/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.client.httpclient;

import java.io.IOException;
import java.util.Map;

import org.sonatype.central.publisher.client.httpclient.auth.AuthProvider;

import org.apache.hc.client5.http.HttpResponseException;

import static java.lang.String.format;
import static org.sonatype.central.publisher.client.PublisherConstants.DELETE_ENDPOINT_URL;
import static org.sonatype.central.publisher.client.httpclient.PublisherHttpClient.sendRequest;
import static org.sonatype.central.publisher.client.httpclient.RequestType.DELETE;
import static org.sonatype.central.publisher.client.httpclient.utils.HttpResponseUtil.toContentString;

public class DeletePublisherEndpoint
{
  public void call(
      final String baseUrl,
      final AuthProvider authProvider,
      final Map<String, String> params,
      final String deploymentId)
  {
    try {
      sendRequest(authProvider, baseUrl + format(DELETE_ENDPOINT_URL, deploymentId), params, null, DELETE);
    }
    catch (HttpResponseException e) {
      throw new RuntimeException("Cannot delete deployment. Response status code: "
          + e.getStatusCode()
          + " response message: "
          + toContentString(e));
    }
    catch (IOException e) {
      throw new RuntimeException("Invalid request. " + e.getMessage());
    }
  }
}
