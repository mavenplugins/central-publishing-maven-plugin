/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin.published;

public interface ComponentPublishedChecker
{
  boolean isComponentPublished(final String groupId, final String artifactId, final String version);
}
