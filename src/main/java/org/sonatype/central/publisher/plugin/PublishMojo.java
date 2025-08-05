/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package org.sonatype.central.publisher.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.sonatype.central.publisher.client.PublisherClient;
import org.sonatype.central.publisher.client.model.PublishingType;
import org.sonatype.central.publisher.plugin.bundler.ArtifactBundler;
import org.sonatype.central.publisher.plugin.config.PlexusContextConfig;
import org.sonatype.central.publisher.plugin.model.ArtifactWithFile;
import org.sonatype.central.publisher.plugin.model.BundleArtifactRequest;
import org.sonatype.central.publisher.plugin.model.ChecksumRequest;
import org.sonatype.central.publisher.plugin.model.StageArtifactRequest;
import org.sonatype.central.publisher.plugin.model.UploadArtifactRequest;
import org.sonatype.central.publisher.plugin.model.WaitForDeploymentStateRequest;
import org.sonatype.central.publisher.plugin.model.WaitUntilRequest;
import org.sonatype.central.publisher.plugin.published.ComponentPublishedChecker;
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

import static java.lang.String.format;
import static org.sonatype.central.publisher.client.PublisherConstants.DEFAULT_ORGANIZATION_ID;
import static org.sonatype.central.publisher.client.httpclient.auth.AuthProviderType.BASIC;
import static org.sonatype.central.publisher.client.httpclient.auth.AuthProviderType.USERTOKEN;
import static org.sonatype.central.publisher.plugin.Constants.*;

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

  @Parameter(property = PUBLISHING_SERVER_ID_NAME, defaultValue = PUBLISHING_SERVER_ID_DEFAULT_VALUE)
  private String publishingServerId;

  @Parameter(property = TOKEN_AUTH_NAME, defaultValue = TOKEN_AUTH_DEFAULT_VALUE)
  private boolean tokenAuth;

  /**
   * Assign whether to auto publish a deployment. Meaning that no manual intervention is required, if a deployment is
   * considered valid, before publishing it. Defaults to {@link Constants#AUTO_PUBLISH_DEFAULT_VALUE}.
   */
  @Parameter(property = AUTO_PUBLISH_NAME, defaultValue = AUTO_PUBLISH_DEFAULT_VALUE)
  private boolean autoPublish;

  /**
   * Assign what to wait for, if desired to wait, of the processing of a deployment. See @{@link WaitUntilRequest} for
   * options. Defaults to {@link Constants#WAIT_MAX_TIME_DEFAULT_VALUE}.
   */
  @Parameter(
      property = WAIT_UNTIL_NAME,
      defaultValue = WAIT_UNTIL_DEFAULT_VALUE
  )
  private String waitUntil;

  /**
   * Assign the amount of seconds that the plugin will wait for a deployment state. Can not be less than
   * {@link Constants#WAIT_MAX_TIME_DEFAULT_VALUE}.
   */
  @Parameter(
      property = WAIT_MAX_TIME_NAME,
      defaultValue = WAIT_MAX_TIME_DEFAULT_VALUE
  )
  private int waitMaxTime;

  /**
   * Assign the amount of seconds between checking whether a deployment has published. Can not be less than
   * {@link Constants#WAIT_POLLING_INTERVAL_DEFAULT_VALUE}.
   */
  @Parameter(
      property = WAIT_POLLING_INTERVAL_NAME,
      defaultValue = WAIT_POLLING_INTERVAL_DEFAULT_VALUE
  )
  private int waitPollingInterval;

  /**
   * @deprecated use {@link #waitPollingInterval} instead
   */
  @Deprecated
  @Parameter(
      property = PUBLISH_COMPLETION_POLL_INTERVAL_NAME,
      defaultValue = PUBLISH_COMPLETION_POLL_INTERVAL_DEFAULT_VALUE
  )
  private int publishCompletionPollInterval;

  /**
   * @deprecated use {@link #autoPublish} in combination with {@link #waitUntil} instead
   */
  @Deprecated
  @Parameter(property = WAIT_FOR_PUBLISH_COMPLETION_NAME, defaultValue = WAIT_FOR_PUBLISH_COMPLETION_DEFAULT_VALUE)
  private boolean waitForPublishCompletion;

  /**
   * Assign the URL that this plugin uses to publish deployments. Defaults to
   * {@link Constants#CENTRAL_BASE_URL_DEFAULT_VALUE}.
   */
  @Parameter(property = CENTRAL_BASE_URL_NAME, defaultValue = CENTRAL_BASE_URL_DEFAULT_VALUE)
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
  @Parameter(property = CHECKSUMS_NAME, defaultValue = CHECKSUMS_DEFAULT_VALUE)
  private String checksums;

  /**
   * Assign whether we ignore, or more specifically, not add components that have already been published in the past to
   * the bundle that will be published. When working with projects that are using a multi-module setup, and it's desired
   * to publish only new modules (for example parent, child1 and child2 are all published, and it's desired to publish a
   * new version X.Y.1 of child2, but leave child1 and parent unchanged), this setting will assure that previous
   * published components will not be published again, which will cause the publishing to fail. Defaults to
   * {@link Constants#IGNORE_PUBLISHED_COMPONENTS_DEFAULT_VALUE}.
   */
  @Parameter(property = IGNORE_PUBLISHED_COMPONENTS_NAME, defaultValue = IGNORE_PUBLISHED_COMPONENTS_DEFAULT_VALUE)
  private boolean ignorePublishedComponents;

  /**
   * Assign artifacts that must not be added to the bundle that represents the deployment that will be published.
   */
  @Parameter(property = EXCLUDE_ARTIFACTS_NAME)
  private List<String> excludeArtifacts = new ArrayList<>();

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

  @Component
  private ComponentPublishedChecker componentPublishedChecker;

  private ChecksumRequest checksumRequest;

  private WaitUntilRequest waitUntilRequest;

  private PublishingType publishingType;

  @Override
  protected void doValidateParameters() throws MojoExecutionException {
    int publishCompletionPollIntervalDefault = Integer.parseInt(PUBLISH_COMPLETION_POLL_INTERVAL_DEFAULT_VALUE);
    int waitPollingIntervalDefault = Integer.parseInt(WAIT_POLLING_INTERVAL_DEFAULT_VALUE);

    // if the default is not used, update waitPollingIntervalDefault.
    if (publishCompletionPollInterval != publishCompletionPollIntervalDefault) {
      getLog().warn(format(
          "%s is deprecated, using it will set %s (converted to seconds).",
          PUBLISH_COMPLETION_POLL_INTERVAL_NAME,
          WAIT_MAX_TIME_NAME
      ));

      // only update waitPollingInterval if it was still set to the default
      if (waitPollingInterval == waitPollingIntervalDefault) {
        // convert to seconds from milliseconds
        waitPollingInterval = (publishCompletionPollInterval / 1000);
      }
    }

    if (waitPollingInterval < waitPollingIntervalDefault) {
      getLog().warn(format(
          "%s was set to be less then %2$s seconds, will use the default of %2$s seconds.",
          WAIT_POLLING_INTERVAL_NAME,
          WAIT_POLLING_INTERVAL_DEFAULT_VALUE
      ));

      waitPollingInterval = waitPollingIntervalDefault;
    }

    int waitMaxTimeDefault = Integer.parseInt(WAIT_MAX_TIME_DEFAULT_VALUE);

    if (waitMaxTime < Integer.parseInt(WAIT_MAX_TIME_DEFAULT_VALUE)) {
      getLog().warn(format(
          "%s was set to be less then %2$s seconds, will use the default of %2$s seconds.",
          WAIT_MAX_TIME_NAME,
          WAIT_MAX_TIME_DEFAULT_VALUE
      ));

      waitMaxTime = waitMaxTimeDefault;
    }

    if (!ChecksumRequest.isValidValue(checksums)) {
      throw new MojoExecutionException(format("%s must be one of the following values %s.",
          CHECKSUMS_NAME,
          ChecksumRequest.toNames())
      );
    }

    checksumRequest = ChecksumRequest.valueOf(checksums.toUpperCase());

    if (!WaitUntilRequest.isValidValue(waitUntil)) {
      throw new MojoExecutionException(format("%s must be one of the following values %s.",
          WAIT_UNTIL_NAME,
          WaitUntilRequest.toNames())
      );
    }

    waitUntilRequest = WaitUntilRequest.valueOf(waitUntil.toUpperCase());
    publishingType = autoPublish ? PublishingType.AUTOMATIC : PublishingType.USER_MANAGED;

    // waitForPublishCompletion is deprecated, but if used, overwrite actually used values.
    if (waitForPublishCompletion) {
      getLog().warn(format(
          "waitForPublishCompletion is deprecated, using it will set %s to true and %s to %s.",
          AUTO_PUBLISH_NAME,
          WAIT_UNTIL_NAME,
          WaitUntilRequest.PUBLISHED.name().toLowerCase()
      ));

      publishingType = PublishingType.AUTOMATIC;
      waitUntilRequest = WaitUntilRequest.PUBLISHED;
    }

    // Are we asked to wait till all was finished publishing while auto publish is disabled? Return and log it.
    if (waitUntilRequest == WaitUntilRequest.PUBLISHED && !autoPublish) {

      getLog().warn(format(
          "Requested to wait for state: %s, but %s is set to %s (default). Waiting only until %s.",
          waitUntilRequest.name().toLowerCase(),
          AUTO_PUBLISH_NAME,
          AUTO_PUBLISH_DEFAULT_VALUE,
          WaitUntilRequest.VALIDATED.name().toLowerCase()
      ));

      waitUntilRequest = WaitUntilRequest.VALIDATED;
    }
  }

  @Override
  protected void doExecute() throws MojoExecutionException {
    File stagingDirectory = getWorkDirectory(forcedStagingDirectory, DEFAULT_STAGING_DIR_NAME);
    File outputDirectory = getWorkDirectory(forcedOutputDirectory, DEFAULT_BUNDLE_OUTPUT_DIR_NAME);
    configurePublisherClient();
    processMavenModule(stagingDirectory);

    if (getMojoUtils().isThisLastProjectWithThisMojoInExecution(getMavenSession(), getMojoExecution(),
        getPluginGroupId(), getPluginArtifactId(), isFailOnBuildFailure())) {
      postProcess(stagingDirectory, outputDirectory, deploymentName);
    }
  }

  protected void processMavenModule(final File stagingDirectory) throws MojoExecutionException {
    final List<ArtifactWithFile> artifactWithFiles =
        getProjectUtils().getArtifacts(getMavenSession().getCurrentProject(), getArtifactFactory());
    artifactWithFiles.removeIf(artifactWithFile -> {
      if (excludeArtifacts.contains(artifactWithFile.getArtifact().getArtifactId())) {
        return true;
      }
      if (ignorePublishedComponents) {
        return componentPublishedChecker.isComponentPublished(artifactWithFile.getArtifact().getGroupId(),
            artifactWithFile.getArtifact().getArtifactId(), artifactWithFile.getArtifact().getVersion());
      }
      return false;
    });

    try {
      artifactStager.stageArtifact(new StageArtifactRequest(artifactWithFiles, stagingDirectory));
      artifactBundler.preBundle(getMavenSession().getCurrentProject(), stagingDirectory.toPath(), checksumRequest);
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
            checksumRequest
        ));

    if (isSkipPublishing()) {
      getLog().info("Skipping Central Publishing at user's request.");
      return;
    }

    UploadArtifactRequest uploadRequest = new UploadArtifactRequest(deploymentName, bundleFile, publishingType);
    String deploymentId = artifactUploader.upload(uploadRequest);

    if (waitUntilRequest == WaitUntilRequest.UPLOADED) {
      outputWhereToFinishPublishing(centralBaseUrl, waitUntilRequest, deploymentId);
      return;
    }

    WaitForDeploymentStateRequest waitForDeploymentStateRequest = new WaitForDeploymentStateRequest(
        centralBaseUrl,
        deploymentId,
        waitUntilRequest,
        waitMaxTime,
        waitPollingInterval
    );

    deploymentPublishedWatcher.waitForDeploymentState(waitForDeploymentStateRequest);
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
    if (tokenAuth) {
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

  private void outputWhereToFinishPublishing(
      final String centralBaseURL,
      final WaitUntilRequest waitUntilRequest,
      final String deploymentId)
  {
    getLog().info(format(
        "Deployment %s has been %s. To finish publishing visit %s/publishing/deployments",
        deploymentId,
        waitUntilRequest.name().toLowerCase(),
        centralBaseURL
    ));
  }
}
