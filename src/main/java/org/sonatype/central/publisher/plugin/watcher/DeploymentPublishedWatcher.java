/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin.watcher;

import org.sonatype.central.publisher.plugin.model.WaitForDeploymentStateRequest;

public interface DeploymentPublishedWatcher
{
  void waitForDeploymentState(WaitForDeploymentStateRequest waitForDeploymentStateRequest);
}
