/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.client.httpclient;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;

import org.sonatype.central.publisher.client.httpclient.auth.AuthProvider;

import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.entity.mime.HttpMultipartMode;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.BasicHttpClientResponseHandler;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.net.URIBuilder;

public class PublisherHttpClient
{
  public static String sendRequest(
      final AuthProvider authProvider,
      final String endpointUrl,
      final Map<String, String> params,
      final Path body,
      final RequestType requestType) throws IOException
  {
    try {
      URIBuilder uriBuilder = new URIBuilder(endpointUrl);
      params.forEach(uriBuilder::addParameter);

      switch (requestType) {
        case POST: {
          HttpPost httpPost = new HttpPost(uriBuilder.build());
          authProvider.getAuthHeaders().forEach(httpPost::addHeader);

          if (body != null) {
            File file = body.toFile();
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setMode(HttpMultipartMode.LEGACY);
            builder.addBinaryBody("bundle", file, ContentType.APPLICATION_OCTET_STREAM, file.getName());
            httpPost.setEntity(builder.build());
          }

          try (CloseableHttpClient client = HttpClients.createDefault()) {
            return client.execute(httpPost, new BasicHttpClientResponseHandler());
          }
        }
        case GET:
        case DELETE:
        default: {
          HttpUriRequestBase httpRequest =
              requestType == RequestType.DELETE ? new HttpDelete(uriBuilder.build()) : new HttpGet(uriBuilder.build());
          authProvider.getAuthHeaders().forEach(httpRequest::addHeader);
          try (CloseableHttpClient client = HttpClients.createDefault()) {
            return client.execute(httpRequest, new BasicHttpClientResponseHandler());
          }
        }
      }
    }
    catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }
}
