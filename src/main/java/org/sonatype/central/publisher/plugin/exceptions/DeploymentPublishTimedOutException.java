/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin.exceptions;

public class DeploymentPublishTimedOutException
    extends RuntimeException
{
  public DeploymentPublishTimedOutException(String deploymentId) {
    super("Polling for " + deploymentId + " timed out before the deployment completed.");
  }
}
