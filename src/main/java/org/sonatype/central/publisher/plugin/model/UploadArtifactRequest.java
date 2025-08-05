/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.model;

import java.nio.file.Path;

import org.sonatype.central.publisher.client.model.PublishingType;

public class UploadArtifactRequest
{
  private final String deploymentName;

  private final Path bundleFile;

  private final PublishingType publishingType;

  public UploadArtifactRequest(
      final String deploymentName,
      final Path bundleFile,
      final PublishingType publishingType
  )
  {
    this.deploymentName = deploymentName;
    this.bundleFile = bundleFile;
    this.publishingType = publishingType;
  }

  public String getDeploymentName() {
    return deploymentName;
  }

  public Path getBundleFile() {
    return bundleFile;
  }

  public PublishingType getPublishingType() {
    return publishingType;
  }
}
