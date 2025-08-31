/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.deleter;

import org.sonatype.central.publisher.client.PublisherClient;
import org.sonatype.central.publisher.client.PublisherClientFactory;
import org.sonatype.central.publisher.client.model.DeploymentApiResponse;
import org.sonatype.central.publisher.client.model.DeploymentState;
import org.sonatype.central.publisher.plugin.exceptions.DeploymentDeleteFailedException;
import org.sonatype.central.publisher.plugin.model.DeleteDeploymentRequest;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import static java.lang.String.format;

@Component(role = DeploymentDeleter.class)
public class DeploymentDeleterImpl
    extends AbstractLogEnabled
    implements DeploymentDeleter
{
  @Requirement
  private PublisherClient publisherClient;

  @SuppressWarnings("unused") // used via reflection by Plexus
  public DeploymentDeleterImpl() {
  }

  DeploymentDeleterImpl(final PublisherClient publisherClient) {
    this.publisherClient = publisherClient != null ? publisherClient : PublisherClientFactory.createPublisherClient();
  }

  @Override
  public void deleteDeployment(DeleteDeploymentRequest deleteDeploymentRequest) {
    if (getLogger() != null) {
      getLogger().info("Going to delete deployment " + deleteDeploymentRequest.getDeploymentId());
    }

    try {
      // Re-check state to avoid deleting once publishing already started.
      final DeploymentApiResponse status = publisherClient.status(deleteDeploymentRequest.getDeploymentId());
      if (status != null && status.getDeploymentState() == DeploymentState.VALIDATED) {
        publisherClient.delete(
            deleteDeploymentRequest.getDeploymentId());

        if (getLogger() != null) {
          getLogger().info(
              format("Deleted deployment successfully, deployment name: %s, deploymentId: %s.",
                  deleteDeploymentRequest.getDeploymentName(), deleteDeploymentRequest.getDeploymentId()));
        }
      }
      else {
        if (getLogger() != null) {
          getLogger().info(format(
              "Skipping delete for deployment %s as state is now %s (expected VALIDATED).",
              deleteDeploymentRequest.getDeploymentId(),
              status != null ? status.getDeploymentState() : "unknown"));
        }
      }
    }
    catch (Exception e) {
      if (getLogger() != null) {
        getLogger().error(format("Unable to delete deployment name: %s, deploymentId: %s",
            deleteDeploymentRequest.getDeploymentName(), deleteDeploymentRequest.getDeploymentId()), e);
      }

      throw new DeploymentDeleteFailedException(deleteDeploymentRequest);
    }
  }
}
