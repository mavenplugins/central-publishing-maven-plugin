/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin.exceptions;

public class ArtifactUploadFailedException
    extends RuntimeException
{
  public ArtifactUploadFailedException(String deploymentName, Exception e) {
    super("Artifact for deployment " + deploymentName + " failed while uploading", e);
  }
}
