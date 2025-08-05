/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin.published;

import java.util.Objects;

import org.sonatype.central.publisher.client.PublisherClient;
import org.sonatype.central.publisher.client.PublisherClientFactory;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.logging.console.ConsoleLogger;

@Component(role = ComponentPublishedChecker.class)
public class ComponentPublishedCheckerImpl
    extends AbstractLogEnabled
    implements ComponentPublishedChecker
{
  @Requirement
  private PublisherClient publisherClient;

  public ComponentPublishedCheckerImpl() {
  }

  public ComponentPublishedCheckerImpl(final PublisherClient publisherClient) {
    this.publisherClient =
        Objects.requireNonNullElseGet(publisherClient, PublisherClientFactory::createPublisherClient);
    if (this.getLogger() == null) {
      this.enableLogging(new ConsoleLogger());
    }
  }

  @Override
  public boolean isComponentPublished(final String groupId, final String artifactId, final String version) {
    getLogger().info(
        "Check component published status for component: groupId:" + groupId + " artifactId:" + artifactId +
            " version:" + version);
    boolean published = publisherClient.isPublished(groupId, artifactId, version);
    if (published) {
      getLogger().info("Excluding component: groupId:" + groupId + " artifactId:" + artifactId + " version:" + version +
          " as a published");
    }
    return published;
  }
}
