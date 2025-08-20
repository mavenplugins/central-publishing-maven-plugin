/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin.config;

import org.sonatype.central.publisher.client.PublisherClient;
import org.sonatype.central.publisher.client.PublisherClientFactory;

import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

@Component(role = PlexusContextConfig.class)
public class PlexusContextConfigImpl
    implements PlexusContextConfig, Contextualizable
{
  @Override
  public void contextualize(final Context context) throws ContextException {
    PlexusContainer plexusContainer = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
    PublisherClient publisherClient = PublisherClientFactory.createPublisherClient();
    plexusContainer.addComponent(publisherClient, PublisherClient.class.getName());
  }
}
