/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.model;

/**
 * Simple class to wrap common request values for requesting to delete a Deployment.
 */
public class DeleteDeploymentRequest
{
  private final String deploymentId;

  private final String deploymentName;

  public DeleteDeploymentRequest(String deploymentId, String deploymentName) {
    this.deploymentId = deploymentId;
    this.deploymentName = deploymentName;
  }

  public String getDeploymentId() {
    return deploymentId;
  }

  public String getDeploymentName() {
    return deploymentName;
  }
}
