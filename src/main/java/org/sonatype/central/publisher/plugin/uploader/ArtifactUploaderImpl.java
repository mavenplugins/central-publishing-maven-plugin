/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.uploader;

import org.sonatype.central.publisher.client.PublisherClient;
import org.sonatype.central.publisher.client.PublisherClientFactory;
import org.sonatype.central.publisher.client.model.PublishingType;
import org.sonatype.central.publisher.plugin.exceptions.DeploymentPublishFailedException;
import org.sonatype.central.publisher.plugin.model.UploadArtifactRequest;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import static java.lang.String.format;
import static java.util.Objects.requireNonNullElseGet;

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
    this.publisherClient = requireNonNullElseGet(publisherClient, PublisherClientFactory::createPublisherClient);
  }

  @Override
  public String upload(final UploadArtifactRequest uploadArtifactRequest) {
    if (getLogger() != null) {
      getLogger().info("Going to upload " + uploadArtifactRequest.getBundleFile());
    }

    try {
      String deploymentId = publisherClient.upload(
          uploadArtifactRequest.getDeploymentName(),
          uploadArtifactRequest.getBundleFile(),
          uploadArtifactRequest.getPublishingType()
      );

      if (getLogger() != null) {
        getLogger().info(
            format("Uploaded bundle successfully, deployment name: %s, deploymentId: %s. Deployment will %s",
                uploadArtifactRequest.getDeploymentName(), deploymentId,
                toPublishingTypeMessage(uploadArtifactRequest))
        );
      }

      return deploymentId;
    }
    catch (Exception e) {
      if (getLogger() != null) {
        getLogger().error("Unable to upload bundle for deployment: " + uploadArtifactRequest.getDeploymentName(), e);
      }

      throw new DeploymentPublishFailedException(uploadArtifactRequest.getDeploymentName());
    }
  }

  private String toPublishingTypeMessage(final UploadArtifactRequest uploadArtifactRequest) {
    PublishingType publishingType = uploadArtifactRequest.getPublishingType();

    switch (publishingType) {
      case USER_MANAGED:
        return "require manual publishing";
      case AUTOMATIC:
        return "publish automatically";
    }

    // should never be reached
    return "";
  }
}
