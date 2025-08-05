/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.client.httpclient;

import java.io.IOException;
import java.util.Map;

import org.sonatype.central.publisher.client.httpclient.auth.AuthProvider;
import org.sonatype.central.publisher.client.model.DeploymentApiResponse;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.HttpResponseException;

import static org.sonatype.central.publisher.client.PublisherConstants.STATUS_ENDPOINT_URL;
import static org.sonatype.central.publisher.client.httpclient.PublisherHttpClient.sendRequest;
import static org.sonatype.central.publisher.client.httpclient.utils.HttpResponseUtil.toContentString;

public class StatusPublisherEndpoint
{
  private static final ObjectMapper objectMapper = new ObjectMapper();

  static {
    objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  public DeploymentApiResponse call(
      final String baseUrl,
      final AuthProvider authProvider,
      final Map<String, String> params)
  {
    try {
      String response = sendRequest(authProvider, baseUrl + STATUS_ENDPOINT_URL, params, null, RequestType.POST);
      return objectMapper.readValue(response, DeploymentApiResponse.class);
    }
    catch (HttpResponseException e) {
      throw new RuntimeException("Cannot get deployment status. Response status code: "
          + e.getStatusCode()
          + " response message: "
          + toContentString(e));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
