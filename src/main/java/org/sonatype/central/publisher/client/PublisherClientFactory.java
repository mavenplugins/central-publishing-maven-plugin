/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.client;

import org.sonatype.central.publisher.client.httpclient.ComponentPublishedEndpoint;
import org.sonatype.central.publisher.client.httpclient.StatusPublisherEndpoint;
import org.sonatype.central.publisher.client.httpclient.UploadPublisherEndpoint;
import org.sonatype.central.publisher.client.httpclient.auth.AuthProviderFactory;

public final class PublisherClientFactory
{
  private PublisherClientFactory() {
  }

  public static PublisherClient createPublisherClient() {
    return new PublisherClientImpl(new UploadPublisherEndpoint(), new StatusPublisherEndpoint(),
        new ComponentPublishedEndpoint(), new AuthProviderFactory());
  }
}
