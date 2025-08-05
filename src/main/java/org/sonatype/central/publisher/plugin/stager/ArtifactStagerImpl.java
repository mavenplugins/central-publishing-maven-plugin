/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.stager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.sonatype.central.publisher.plugin.model.ArtifactWithFile;
import org.sonatype.central.publisher.plugin.model.StageArtifactRequest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.artifact.installer.ArtifactInstaller;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryFactory;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;

import static org.sonatype.central.publisher.plugin.Constants.LOCAL_STAGING_REPOSITORY_NAME;

@Component(role = ArtifactStager.class)
public class ArtifactStagerImpl
    extends AbstractLogEnabled
    implements ArtifactStager
{
  private static final Object parallelLock = new Object();

  @Requirement
  private ArtifactInstaller artifactInstaller;

  @Requirement
  private ArtifactRepositoryFactory artifactRepositoryFactory;

  @Requirement
  private ArtifactRepositoryLayout artifactRepositoryLayout;

  @Override
  public void stageArtifact(final StageArtifactRequest stageArtifactRequest)
      throws MojoExecutionException, ArtifactInstallationException
  {
    if (!stageArtifactRequest.getArtifactWithFiles().isEmpty()) {
      getLogger().info("Staging " + stageArtifactRequest.getArtifactWithFiles().size() + " files");
      final File stagingDirectory = stageArtifactRequest.getStagingDirectory();

      // let maven 'install' the content into a repository, guarantee proper placement of all artifacts
      final ArtifactRepository stagingRepository = getStagingArtifactRepository(stagingDirectory);

      for (ArtifactWithFile artifactWithFile : stageArtifactRequest.getArtifactWithFiles()) {
        install(artifactWithFile.getFile(), artifactWithFile.getArtifact(), stagingRepository);
      }
    }
    else {
      getLogger().info("No files to stage!");
    }
  }

  protected void install(final File source, final Artifact artifact, final ArtifactRepository stagingRepository)
      throws ArtifactInstallationException
  {
    synchronized (parallelLock) {
      getLogger().info("Staging " + source.getAbsolutePath());
      artifactInstaller.install(source, artifact, stagingRepository);
    }
  }

  protected ArtifactRepository getStagingArtifactRepository(final File stagingDirectory) throws MojoExecutionException {
    if (stagingDirectory == null) {
      throw new MojoExecutionException("Staging failed: staging directory is null!");
    }

    try {
      Files.createDirectories(stagingDirectory.toPath());
      final String url = stagingDirectory.getCanonicalFile().toURI().toURL().toExternalForm();
      return createStagingArtifactRepository(url);
    }
    catch (IOException e) {
      throw new MojoExecutionException(
          "Staging failed: could not create ArtifactRepository in staging directory " + stagingDirectory, e);
    }
  }

  protected ArtifactRepository createStagingArtifactRepository(final String url) {
    return artifactRepositoryFactory.createDeploymentArtifactRepository(LOCAL_STAGING_REPOSITORY_NAME, url,
        artifactRepositoryLayout, true);
  }
}
