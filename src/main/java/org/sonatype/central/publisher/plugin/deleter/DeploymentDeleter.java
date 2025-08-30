/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.deleter;

import org.sonatype.central.publisher.plugin.model.DeleteDeploymentRequest;

public interface DeploymentDeleter
{
  void deleteDeployment(
      final DeleteDeploymentRequest deleteDeploymentRequest);
}
