/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.stager;

import org.sonatype.central.publisher.plugin.model.StageArtifactRequest;

import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.plugin.MojoExecutionException;

public interface ArtifactStager
{
  void stageArtifact(final StageArtifactRequest stageArtifactRequest)
      throws MojoExecutionException, ArtifactInstallationException;
}
