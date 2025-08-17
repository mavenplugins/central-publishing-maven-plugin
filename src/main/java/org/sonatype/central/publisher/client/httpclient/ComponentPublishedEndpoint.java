/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.client.httpclient;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.sonatype.central.publisher.client.httpclient.auth.AuthProvider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.HttpResponseException;

import static org.sonatype.central.publisher.client.PublisherConstants.PUBLISHED_ENDPOINT_URL;
import static org.sonatype.central.publisher.client.httpclient.PublisherHttpClient.sendRequest;
import static org.sonatype.central.publisher.client.httpclient.utils.HttpResponseUtil.toContentString;

public class ComponentPublishedEndpoint
{
  private final ObjectMapper objectMapper = new ObjectMapper();

  public boolean call(
      final String baseUrl,
      final AuthProvider authProvider,
      final Map<String, String> params)
  {
    try {
      String response = sendRequest(authProvider, baseUrl + PUBLISHED_ENDPOINT_URL, params, null, RequestType.GET);
      Map<String, Boolean> result = objectMapper.readValue(response, new TypeReference<HashMap<String, Boolean>>() { });
      return result.get("published");
    }
    catch (HttpResponseException e) {
      throw new RuntimeException("Cannot get component published status. Response status code: "
          + e.getStatusCode()
          + " response message: "
          + toContentString(e));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
