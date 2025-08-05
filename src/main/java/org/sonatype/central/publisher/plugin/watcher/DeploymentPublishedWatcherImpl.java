/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin.watcher;

import java.util.List;
import java.util.Map.Entry;

import org.sonatype.central.publisher.client.PublisherClient;
import org.sonatype.central.publisher.client.model.DeploymentApiResponse;
import org.sonatype.central.publisher.client.model.DeploymentState;
import org.sonatype.central.publisher.plugin.exceptions.DeploymentPublishFailedException;
import org.sonatype.central.publisher.plugin.exceptions.DeploymentPublishTimedOutException;
import org.sonatype.central.publisher.plugin.utils.PurlUtils;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;

@Component(role = DeploymentPublishedWatcher.class)
public class DeploymentPublishedWatcherImpl
    extends AbstractLogEnabled
    implements DeploymentPublishedWatcher
{
  public static final int MAX_RETRIES = 100;

  @Requirement
  private PublisherClient publisherClient;

  @Requirement
  private PurlUtils purlUtils;

  @SuppressWarnings("unused") // used via reflection by Plexus
  public DeploymentPublishedWatcherImpl() { }

  DeploymentPublishedWatcherImpl(PublisherClient publisherClient, PurlUtils purlUtils) {
    this.publisherClient = publisherClient;
    this.purlUtils = purlUtils;
  }

  @Override
  public void waitForPublishCompletion(final String deploymentId, final int publishCompletionPollInterval) {
    getLogger().debug("Waiting for Deployment state to move to PUBLISHED");
    DeploymentApiResponse status = null;
    for (int i = 0; i < MAX_RETRIES; ++i) {
      try {
        Thread.sleep(publishCompletionPollInterval);
        getLogger().debug("Requesting status for Deployment " + deploymentId);
        status = publisherClient.status(deploymentId);
        DeploymentState deploymentState = status.getDeploymentState();
        getLogger().debug("Deployment " + deploymentId + " in state " + deploymentState);
        if (DeploymentState.PUBLISHED == deploymentState) {
          outputSuccess(status);
          return;
        }
        else if (DeploymentState.FAILED == deploymentState) {
          outputError(status);
        }
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    }

    // exceeded MAX_RETRIES
    outputTimeout(deploymentId, status);
  }

  private void outputSuccess(final DeploymentApiResponse status) {
    StringBuilder successMessage = new StringBuilder();
    successMessage.append("Deployment ").append(status.getDeploymentId()).append(" was successfully published\n")
        .append("Packages\n");
    for (String purl : status.getPurls()) {
      String purlDisplay = purlUtils.toRepo1Url(purl).orElse(purl);
      successMessage.append(" - ").append(purlDisplay).append("\n");
    }
    successMessage.append("CherryBomb Report: ").append(status.getCherryBomUrl());
    if (null != getLogger()) {
      getLogger().info(successMessage.toString());
    }
  }

  private void outputError(final DeploymentApiResponse status) {
    StringBuilder errorMessage = new StringBuilder();
    errorMessage.append("Deployment ").append(status.getDeploymentId()).append(" failed while publishing\n");

    for (Entry<String, List<String>> errorCategory : status.getErrors().entrySet()) {
      errorMessage.append(errorCategory.getKey()).append(":\n");
      for (String error : errorCategory.getValue()) {
        errorMessage.append(" - ").append(error).append("\n");
      }
      errorMessage.append("\n");
    }

    if (null != getLogger()) {
      getLogger().error(errorMessage.toString());
    }

    throw new DeploymentPublishFailedException(status.getDeploymentId(), status.getDeploymentName());
  }

  private void outputTimeout(final String deploymentId, final DeploymentApiResponse status) {
    if (null != getLogger() && null != status) {
      getLogger().debug("Deployment " + deploymentId + " timed out with the last recorded status of: " + status.getDeploymentState());
    }
    throw new DeploymentPublishTimedOutException(deploymentId);
  }
}
