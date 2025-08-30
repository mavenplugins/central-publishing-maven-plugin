/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin.exceptions;

import org.sonatype.central.publisher.plugin.model.DeleteDeploymentRequest;

public class DeploymentDeleteFailedException
    extends RuntimeException
{
  public DeploymentDeleteFailedException(final DeleteDeploymentRequest deleteDeploymentRequest) {
    super("Deployment " + deleteDeploymentRequest.getDeploymentId()
        + " (" + deleteDeploymentRequest.getDeploymentName() + ") failed while deleting");
  }
}
