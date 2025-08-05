/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.uploader;

import java.util.Objects;

import org.sonatype.central.publisher.client.PublisherClient;
import org.sonatype.central.publisher.client.PublisherClientFactory;
import org.sonatype.central.publisher.plugin.model.UploadArtifactRequest;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;

@Component(role = ArtifactUploader.class)
public class ArtifactUploaderImpl
    extends AbstractLogEnabled
    implements ArtifactUploader
{
  @Requirement
  private PublisherClient publisherClient;

  @SuppressWarnings("unused") // used via reflection by Plexus
  public ArtifactUploaderImpl() { }

  ArtifactUploaderImpl(final PublisherClient publisherClient) {
    this.publisherClient =
        Objects.requireNonNullElseGet(publisherClient, PublisherClientFactory::createPublisherClient);
  }

  @Override
  public String upload(final UploadArtifactRequest uploadArtifactRequest) {
    if (getLogger() != null) {
      getLogger().info("Going to upload " + uploadArtifactRequest.getBundleFile());
    }
    try {
      String deploymentId =
          publisherClient.upload(uploadArtifactRequest.getDeploymentName(), uploadArtifactRequest.getBundleFile(), uploadArtifactRequest.getPublishingType());
      if (getLogger() != null) {
        getLogger().info("Uploaded bundle successfully, deployment name: " + uploadArtifactRequest.getDeploymentName() +
            ", deploymentId: " + deploymentId);
      }
      return deploymentId;
    }
    catch (Exception e) {
      if (getLogger() != null) {
        getLogger().error("Unable to upload bundle for deployment: " + uploadArtifactRequest.getDeploymentName(), e);
      }
      return null;
    }
  }
}
