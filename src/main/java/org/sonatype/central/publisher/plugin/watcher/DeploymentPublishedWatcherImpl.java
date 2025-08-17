/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin.watcher;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map.Entry;

import org.sonatype.central.publisher.client.PublisherClient;
import org.sonatype.central.publisher.client.model.DeploymentApiResponse;
import org.sonatype.central.publisher.client.model.DeploymentState;
import org.sonatype.central.publisher.plugin.exceptions.DeploymentPublishFailedException;
import org.sonatype.central.publisher.plugin.exceptions.DeploymentPublishTimedOutException;
import org.sonatype.central.publisher.plugin.model.WaitForDeploymentStateRequest;
import org.sonatype.central.publisher.plugin.model.WaitUntilRequest;
import org.sonatype.central.publisher.plugin.utils.PurlUtils;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component(role = DeploymentPublishedWatcher.class)
public class DeploymentPublishedWatcherImpl
    extends AbstractLogEnabled
    implements DeploymentPublishedWatcher
{
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
  public void waitForDeploymentState(WaitForDeploymentStateRequest waitForDeploymentStateRequest) {
    Instant start = Instant.now();

    String deploymentId = waitForDeploymentStateRequest.getDeploymentId();
    WaitUntilRequest waitUntilRequest = waitForDeploymentStateRequest.getWaitUntilRequest();
    DeploymentApiResponse status = null;

    getLogger().info(format(
        "Waiting until Deployment %s is %s", deploymentId, waitForDeploymentStateRequest.waitTypeName()));

    try {
      while (Duration.between(start, Instant.now()).get(SECONDS) <
          waitForDeploymentStateRequest.getWaitMaxTimeInSeconds()) {

        // convert to milliseconds
        Integer interval = waitForDeploymentStateRequest.getWaitPollingIntervalInSeconds() * 1000;

        Thread.sleep(interval.longValue());

        getLogger().debug("Requesting status for Deployment " + deploymentId);

        status = publisherClient.status(deploymentId);
        DeploymentState deploymentState = status.getDeploymentState();

        getLogger().debug(format("Deployment %s in state: %s", deploymentId, deploymentState.name().toLowerCase()));

        switch (deploymentState) {
          case PENDING:
          case VALIDATING:
            if (waitUntilRequest == WaitUntilRequest.UPLOADED) {
              outputWhereToFinishPublishing(waitForDeploymentStateRequest, deploymentId);
              return;
            }
            break;
          case VALIDATED:
          case PUBLISHING:
            if (waitUntilRequest == WaitUntilRequest.UPLOADED || waitUntilRequest == WaitUntilRequest.VALIDATED) {
              outputWhereToFinishPublishing(waitForDeploymentStateRequest, deploymentId);
              return;
            }
            break;
          case PUBLISHED:
            outputPublished(status);
            return;
          case FAILED:
            outputError(status);
            return;
        }
      }
    }
    catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // exceeds timeout
    outputTimeout(deploymentId, status);
  }

  private void outputPublished(final DeploymentApiResponse status) {
    StringBuilder successMessage = new StringBuilder();

    successMessage
        .append("Deployment ")
        .append(status.getDeploymentId())
        .append(" was successfully published\n")
        .append("Packages\n");

    for (String purl : status.getPurls()) {
      String purlDisplay = purlUtils.toRepo1Url(purl).orElse(purl);
      successMessage.append(" - ").append(purlDisplay).append("\n");
    }

    String cherryBomUrl = status.getCherryBomUrl();
    if (isNotBlank(cherryBomUrl)) {
      successMessage.append("CherryBomb Report: ").append(cherryBomUrl);
    }

    getLogger().info(successMessage.toString());
  }

  private void outputError(final DeploymentApiResponse status) {
    StringBuilder errorMessage = new StringBuilder();

    errorMessage
        .append("\n\n")
        .append("Deployment ")
        .append(status.getDeploymentId())
        .append(" failed\n");

    for (Entry<String, List<String>> errorCategory : status.getErrors().entrySet()) {
      errorMessage.append(errorCategory.getKey()).append(":\n");
      for (String error : errorCategory.getValue()) {
        errorMessage.append(" - ").append(error).append("\n");
      }
      errorMessage.append("\n");
    }

    getLogger().error(errorMessage.toString());

    throw new DeploymentPublishFailedException(status.getDeploymentId(), status.getDeploymentName());
  }

  private void outputTimeout(final String deploymentId, final DeploymentApiResponse status) {
    if (null != status) {
      getLogger().debug(format("Deployment %s timed out with the last recorded status of: %s", deploymentId,
          status.getDeploymentState()));
    }

    throw new DeploymentPublishTimedOutException(deploymentId);
  }

  private void outputWhereToFinishPublishing(
      final WaitForDeploymentStateRequest waitForDeploymentStateRequest,
      final String deploymentId)
  {
    getLogger().info(format(
        "Deployment %s has been %s. To finish publishing visit %s/publishing/deployments",
        deploymentId,
        waitForDeploymentStateRequest.waitTypeName(),
        waitForDeploymentStateRequest.getCentralBaseUrl()
    ));
  }
}
