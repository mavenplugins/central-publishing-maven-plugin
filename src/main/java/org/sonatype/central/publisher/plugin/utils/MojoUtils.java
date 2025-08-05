/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.utils;

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;

public interface MojoUtils
{
  boolean isThisLastProjectWithThisMojoInExecution(
      final MavenSession mavenSession,
      final MojoExecution mojoExecution,
      final String pluginGroupId,
      final String pluginArtifactId,
      final boolean failOnBuildFailure);

  boolean isThisFirstProjectWithThisMojoInExecution(
      final MavenSession mavenSession,
      final MojoExecution mojoExecution,
      final String pluginGroupId,
      final String pluginArtifactId);

  File getWorkDirectoryRoot(
      final String relativePath,
      final MavenSession mavenSession,
      final String pluginGroupId,
      final String pluginArtifactId,
      final String goal);
}
