/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.client;

import java.nio.file.Path;

import org.sonatype.central.publisher.client.httpclient.auth.AuthProvider;
import org.sonatype.central.publisher.client.httpclient.auth.AuthProviderType;
import org.sonatype.central.publisher.client.httpclient.utils.PublisherBundle;
import org.sonatype.central.publisher.client.model.DeploymentApiResponse;
import org.sonatype.central.publisher.client.model.PublishingType;

public interface PublisherClient
{
  PublisherBundle compose(final Path sourceDir, final String bundleFileName);

  PublisherBundle compose(final Path sourceDir, final Path destDir, final String bundleFileName);

  PublisherBundle.BundleBuilder getBuilder(final Path sourceDir);

  String upload(final String name, final Path body, final PublishingType publishingType);

  DeploymentApiResponse status(final String deploymentId);

  boolean isPublished(final String namespace, final String name, final String version);

  /**
   * For auth providers whose principal is not a userId
   */
  AuthProvider setAuthProvider(
      final AuthProviderType authProviderType,
      final String organizationId,
      final String userId,
      final String principal,
      final String credential);

  /**
   * For auth providers whose principal is a userId
   */
  default AuthProvider setAuthProvider(
      final AuthProviderType authProviderType,
      final String organizationId,
      final String principal,
      final String credential)
  {
    return setAuthProvider(authProviderType, organizationId, principal, principal, credential);
  }

  void setCentralBaseUrl(final String centralBaseUrl);
}
