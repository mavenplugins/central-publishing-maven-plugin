/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.client.httpclient.utils;

import org.apache.hc.client5.http.HttpResponseException;

public class HttpResponseUtil
{
  private HttpResponseUtil() {

  }

  public static String toContentString(HttpResponseException e) {
    byte[] contentBytes = e.getContentBytes();
    return contentBytes != null ? new String(contentBytes) : "";
  }
}
