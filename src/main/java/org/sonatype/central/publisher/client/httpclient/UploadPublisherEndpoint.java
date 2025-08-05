/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.client.httpclient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import org.sonatype.central.publisher.client.httpclient.auth.AuthProvider;

import org.apache.hc.client5.http.HttpResponseException;

import static org.sonatype.central.publisher.client.PublisherConstants.UPLOAD_ENDPOINT_URL;
import static org.sonatype.central.publisher.client.httpclient.PublisherHttpClient.sendRequest;
import static org.sonatype.central.publisher.client.httpclient.RequestType.POST;
import static org.sonatype.central.publisher.client.httpclient.utils.HttpResponseUtil.toContentString;

public class UploadPublisherEndpoint
{
  public String call(
      final String baseUrl,
      final AuthProvider authProvider,
      final Map<String, String> params,
      final Path body)
  {
    try {
      return sendRequest(authProvider, baseUrl + UPLOAD_ENDPOINT_URL, params, body, POST);
    }
    catch (HttpResponseException e) {
      throw new RuntimeException(
          "Invalid request. Status: " + e.getStatusCode() + " Response body: " + toContentString(e));
    }
    catch (IOException e) {
      throw new RuntimeException("Invalid request. " + e.getMessage());
    }
  }
}
