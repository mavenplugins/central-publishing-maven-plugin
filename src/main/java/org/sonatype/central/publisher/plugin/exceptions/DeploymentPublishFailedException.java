/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin.exceptions;

public class DeploymentPublishFailedException
    extends RuntimeException
{

  public DeploymentPublishFailedException(String deploymentId, String deploymentName) {
    super("Deployment " + deploymentId + " (" + deploymentName + ") failed while publishing");
  }

  public DeploymentPublishFailedException(String deploymentName) {
    super("Deployment " + deploymentName + " failed while publishing");
  }
}
