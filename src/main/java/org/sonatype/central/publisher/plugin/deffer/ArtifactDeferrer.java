/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.deffer;

import java.io.File;
import java.io.IOException;

import org.sonatype.central.publisher.plugin.model.DeferArtifactRequest;

import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Interface for deferring artifact installation and deployment.
 */
public interface ArtifactDeferrer
{
  /**
   * Install artifact with files.
   *
   * @param artifactWithFiles artifact with files to install
   * @throws ArtifactInstallationException if installation fails
   * @throws MojoExecutionException if execution fails
   */
  void install(DeferArtifactRequest artifactWithFiles) throws ArtifactInstallationException, MojoExecutionException;

  /**
   * Deploy artifact.
   *
   * @param mavenSession - {@link MavenSession}
   * @param sourceDirectory - {@link File}
   * @param remoteRepository - {@link ArtifactRepository}
   * @throws ArtifactDeploymentException if deployment fails
   * @throws IOException if reading of index fails
   */
  @SuppressWarnings("deprecation")
  void deployUp(
      MavenSession mavenSession,
      File sourceDirectory,
      ArtifactRepository remoteRepository) throws ArtifactDeploymentException, IOException;
}
