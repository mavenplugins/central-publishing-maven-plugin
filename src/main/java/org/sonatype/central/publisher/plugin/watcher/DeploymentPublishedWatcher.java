/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin.watcher;

public interface DeploymentPublishedWatcher
{

  void waitForPublishCompletion(String deploymentId, int publishCompletionPollInterval);
}
