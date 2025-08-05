/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.sonatype.central.publisher.client.PublisherClient;
import org.sonatype.central.publisher.client.model.PublishingType;
import org.sonatype.central.publisher.plugin.bundler.ArtifactBundler;
import org.sonatype.central.publisher.plugin.config.PlexusContextConfig;
import org.sonatype.central.publisher.plugin.exceptions.DeploymentPublishFailedException;
import org.sonatype.central.publisher.plugin.model.ArtifactWithFile;
import org.sonatype.central.publisher.plugin.model.BundleArtifactRequest;
import org.sonatype.central.publisher.plugin.model.ChecksumRequest;
import org.sonatype.central.publisher.plugin.model.StageArtifactRequest;
import org.sonatype.central.publisher.plugin.model.UploadArtifactRequest;
import org.sonatype.central.publisher.plugin.stager.ArtifactStager;
import org.sonatype.central.publisher.plugin.uploader.ArtifactUploader;
import org.sonatype.central.publisher.plugin.utils.AuthData;
import org.sonatype.central.publisher.plugin.watcher.DeploymentPublishedWatcher;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;

import static org.sonatype.central.publisher.client.PublisherConstants.DEFAULT_ORGANIZATION_ID;
import static org.sonatype.central.publisher.client.httpclient.auth.AuthProviderType.BASIC;
import static org.sonatype.central.publisher.client.httpclient.auth.AuthProviderType.USERTOKEN;
import static org.sonatype.central.publisher.plugin.Constants.DEFAULT_BUNDLE_OUTPUT_DIR_NAME;
import static org.sonatype.central.publisher.plugin.Constants.DEFAULT_BUNDLE_OUTPUT_FILENAME;
import static org.sonatype.central.publisher.plugin.Constants.DEFAULT_DEPLOYMENT_NAME;
import static org.sonatype.central.publisher.plugin.Constants.DEFAULT_STAGING_DIR_NAME;
import static org.sonatype.central.publisher.plugin.model.ChecksumRequest.ALL;

@Mojo(name = "publish", defaultPhase = LifecyclePhase.DEPLOY, requiresOnline = true, threadSafe = true)
public class PublishMojo
    extends AbstractPublisherMojo
{
  @Parameter(property = "outputFilename", defaultValue = DEFAULT_BUNDLE_OUTPUT_FILENAME, required = true)
  private String outputFilename;

  @Parameter(property = "outputDirectory")
  private File forcedOutputDirectory;

  @Parameter(property = "stagingDirectory")
  private File forcedStagingDirectory;

  @Parameter(property = "deploymentName", defaultValue = DEFAULT_DEPLOYMENT_NAME)
  private String deploymentName;

  @Parameter(defaultValue = "${publishing.server.id}", readonly = true)
  private String publishingServerId;

  @Parameter(property = "tokenAuth", defaultValue = "false", readonly = true)
  private boolean tokenEnabled;

  @Parameter(property = "waitForPublishCompletion", defaultValue = "false")
  private boolean waitForPublishCompletion;

  @Parameter(property = "publishCompletionPollInterval", defaultValue = "1000")
  private int publishCompletionPollInterval;

  @Parameter(property = "centralBaseUrl")
  private String centralBaseUrl;

  /**
   * Assign what type of checksums will be generated for files. Three options are available:
   * <p/>
   * all - Will request MD5, SHA1, SHA256 and SHA512 to be generated.
   * <p/>
   * required - Only MD5 and SHA1 will be requested to be generated.
   * <p/>
   * none - No Checksums will be requested to be generated.
   * <p/>
   */
  @Parameter(property = "checksums", defaultValue = "ALL")
  private String checksums;

  @Component
  private PlexusContextConfig plexusContextConfig;

  @Component
  private ArtifactBundler artifactBundler;

  @Component
  private ArtifactStager artifactStager;

  @Component
  private PublisherClient publisherClient;

  @Component
  private ArtifactUploader artifactUploader;

  @Component
  private DeploymentPublishedWatcher deploymentPublishedWatcher;

  @Override
  protected void doExecute() throws MojoExecutionException {
    File stagingDirectory = getWorkDirectory(forcedStagingDirectory, DEFAULT_STAGING_DIR_NAME);
    File outputDirectory = getWorkDirectory(forcedOutputDirectory, DEFAULT_BUNDLE_OUTPUT_DIR_NAME);

    processMavenModule(stagingDirectory);

    if (getMojoUtils().isThisLastProjectWithThisMojoInExecution(getMavenSession(), getMojoExecution(),
        getPluginGroupId(), getPluginArtifactId(), isFailOnBuildFailure())) {
      postProcess(stagingDirectory, outputDirectory, deploymentName);
    }
  }

  protected void processMavenModule(final File stagingDirectory) throws MojoExecutionException {
    final List<ArtifactWithFile> artifactWithFiles =
        getProjectUtils().getArtifacts(getMavenSession().getCurrentProject(), getArtifactFactory());

    try {
      artifactStager.stageArtifact(new StageArtifactRequest(artifactWithFiles, stagingDirectory));
      artifactBundler.preBundle(getMavenSession().getCurrentProject(),
          stagingDirectory.toPath(), ChecksumRequest.valueOf(checksums, ALL));
    }
    catch (final ArtifactInstallationException e) {
      throw new MojoExecutionException(e);
    }
  }

  protected void postProcess(final File stagingDirectory, final File outputDirectory, final String deploymentName) {

    Path bundleFile = artifactBundler.bundle(
        new BundleArtifactRequest(
            getMavenSession().getCurrentProject(),
            stagingDirectory,
            outputDirectory,
            outputFilename,
            ChecksumRequest.valueOf(checksums, ALL)
        ));

    PublishingType publishingType = PublishingType.USER_MANAGED;
    if (waitForPublishCompletion) {
      publishingType = PublishingType.AUTOMATIC;
    }
    UploadArtifactRequest uploadRequest =
        new UploadArtifactRequest(deploymentName, bundleFile, publishingType);
    configurePublisherClient();
    String deploymentId = artifactUploader.upload(uploadRequest);
    if (waitForPublishCompletion) {
      // TODO: Move this into the catch block of ArtifactUploaderImpl as a part of CDV-2088
      if (null == deploymentId) {
        throw new DeploymentPublishFailedException(deploymentName);
      }
      deploymentPublishedWatcher.waitForPublishCompletion(deploymentId, publishCompletionPollInterval);
    }
  }

  /**
   * Gets the working directory for the plugin. Defaults to under the target folder unless overridden. Synchronized
   * because the first execution of this plugin cleans the working directory.
   *
   * @param forcedWorkDirectory an override File that points to a valid directory
   * @param relativePath        a path relative to the working directory root for the folder
   * @return a File that points to a valid empty directory for the plugin to use as temporary working storage
   */
  protected synchronized File getWorkDirectory(final File forcedWorkDirectory, final String relativePath) {
    File workDirectory = forcedWorkDirectory;
    if (workDirectory == null) {
      workDirectory = getMojoUtils().getWorkDirectoryRoot(relativePath, getMavenSession(), getPluginGroupId(),
          getPluginArtifactId(), null);
    }

    if (getMojoUtils().isThisFirstProjectWithThisMojoInExecution(getMavenSession(), getMojoExecution(),
        getPluginGroupId(), getPluginArtifactId())) {
      ensureCleanDirectory(workDirectory);
    }

    return workDirectory;
  }

  private void ensureCleanDirectory(File workingDirectory) {
    if (workingDirectory.exists() && workingDirectory.isDirectory()) {
      try {
        FileUtils.deleteDirectory(workingDirectory);
        getLog().debug("Deleted existing working directory: " + workingDirectory);
      }
      catch (IOException e) {
        throw new RuntimeException("Failed to clean up working directory before plugin execution", e);
      }
    }
    try {
      Path workingDirectoryPath = workingDirectory.toPath();
      Files.createDirectories(workingDirectoryPath);
      getLog().debug("Created working directory: " + workingDirectoryPath);
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to create empty working directory before plugin execution", e);
    }
  }

  private void configurePublisherClient() {
    if (centralBaseUrl != null) {
      getLog().info("Using Central baseUrl: " + centralBaseUrl);
      publisherClient.setCentralBaseUrl(centralBaseUrl);
    }

    getLog().info("Using credentials from server id " + publishingServerId + " in settings.xml");

    AuthData authData = getUserCredentials();
    if (tokenEnabled) {
      getLog().info("Using Usertoken auth, with namecode: " + authData.getUsername());
      publisherClient.setAuthProvider(USERTOKEN, DEFAULT_ORGANIZATION_ID,
          authData.getUsername(),
          authData.getPassword());
    }
    else {
      getLog().info("Using Basic auth, with username: " + authData.getUsername());
      publisherClient.setAuthProvider(BASIC, DEFAULT_ORGANIZATION_ID,
          authData.getUsername(),
          authData.getPassword());
    }
  }

  private AuthData getUserCredentials() {
    try {
      Server server = getMavenSession().getSettings().getServer(publishingServerId);
      return new AuthData(server.getUsername(), server.getPassword());
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to get publisher server properties for server id: " + publishingServerId, e);
    }
  }
}
