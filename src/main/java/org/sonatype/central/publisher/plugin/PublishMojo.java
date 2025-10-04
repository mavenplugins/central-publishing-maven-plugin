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
import org.sonatype.central.publisher.plugin.deffer.ArtifactDeferrer;
import org.sonatype.central.publisher.plugin.deffer.ArtifactDeferrerImpl;
import org.sonatype.central.publisher.plugin.deleter.DeploymentDeleter;
import org.sonatype.central.publisher.plugin.model.ArtifactWithFile;
import org.sonatype.central.publisher.plugin.model.BundleArtifactRequest;
import org.sonatype.central.publisher.plugin.model.ChecksumRequest;
import org.sonatype.central.publisher.plugin.model.DeferArtifactRequest;
import org.sonatype.central.publisher.plugin.model.DeleteDeploymentRequest;
import org.sonatype.central.publisher.plugin.model.StageArtifactRequest;
import org.sonatype.central.publisher.plugin.model.UploadArtifactRequest;
import org.sonatype.central.publisher.plugin.model.WaitForDeploymentStateRequest;
import org.sonatype.central.publisher.plugin.model.WaitUntilRequest;
import org.sonatype.central.publisher.plugin.published.ComponentPublishedChecker;
import org.sonatype.central.publisher.plugin.stager.ArtifactStager;
import org.sonatype.central.publisher.plugin.uploader.ArtifactUploader;
import org.sonatype.central.publisher.plugin.utils.AuthData;
import org.sonatype.central.publisher.plugin.utils.DirectoryUtils;
import org.sonatype.central.publisher.plugin.watcher.DeploymentPublishedWatcher;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.deployer.ArtifactDeploymentException;
import org.apache.maven.artifact.installer.ArtifactInstallationException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.sonatype.central.publisher.client.PublisherConstants.DEFAULT_ORGANIZATION_ID;
import static org.sonatype.central.publisher.client.httpclient.auth.AuthProviderType.USERTOKEN;
import static org.sonatype.central.publisher.plugin.Constants.AUTO_PUBLISH_DEFAULT_VALUE;
import static org.sonatype.central.publisher.plugin.Constants.AUTO_PUBLISH_NAME;
import static org.sonatype.central.publisher.plugin.Constants.CENTRAL_BASE_URL_DEFAULT_VALUE;
import static org.sonatype.central.publisher.plugin.Constants.CENTRAL_BASE_URL_NAME;
import static org.sonatype.central.publisher.plugin.Constants.CENTRAL_SNAPSHOTS_URL_NAME;
import static org.sonatype.central.publisher.plugin.Constants.CHECKSUMS_DEFAULT_VALUE;
import static org.sonatype.central.publisher.plugin.Constants.CHECKSUMS_NAME;
import static org.sonatype.central.publisher.plugin.Constants.DEFAULT_BUNDLE_OUTPUT_DIR_NAME;
import static org.sonatype.central.publisher.plugin.Constants.DEFAULT_BUNDLE_OUTPUT_FILENAME;
import static org.sonatype.central.publisher.plugin.Constants.DEFAULT_DEFERRED_DIR_NAME;
import static org.sonatype.central.publisher.plugin.Constants.DEFAULT_DEPLOYMENT_NAME;
import static org.sonatype.central.publisher.plugin.Constants.DEFAULT_STAGING_DIR_NAME;
import static org.sonatype.central.publisher.plugin.Constants.DROP_VALIDATED_DEFAULT_VALUE;
import static org.sonatype.central.publisher.plugin.Constants.DROP_VALIDATED_NAME;
import static org.sonatype.central.publisher.plugin.Constants.EXCLUDE_ARTIFACTS_NAME;
import static org.sonatype.central.publisher.plugin.Constants.IGNORE_PUBLISHED_COMPONENTS_DEFAULT_VALUE;
import static org.sonatype.central.publisher.plugin.Constants.IGNORE_PUBLISHED_COMPONENTS_NAME;
import static org.sonatype.central.publisher.plugin.Constants.PUBLISHING_SERVER_ID_DEFAULT_VALUE;
import static org.sonatype.central.publisher.plugin.Constants.PUBLISHING_SERVER_ID_NAME;
import static org.sonatype.central.publisher.plugin.Constants.PUBLISH_COMPLETION_POLL_INTERVAL_DEFAULT_VALUE;
import static org.sonatype.central.publisher.plugin.Constants.PUBLISH_COMPLETION_POLL_INTERVAL_NAME;
import static org.sonatype.central.publisher.plugin.Constants.WAIT_FOR_PUBLISH_COMPLETION_DEFAULT_VALUE;
import static org.sonatype.central.publisher.plugin.Constants.WAIT_FOR_PUBLISH_COMPLETION_NAME;
import static org.sonatype.central.publisher.plugin.Constants.WAIT_MAX_TIME_DEFAULT_VALUE;
import static org.sonatype.central.publisher.plugin.Constants.WAIT_MAX_TIME_NAME;
import static org.sonatype.central.publisher.plugin.Constants.WAIT_POLLING_INTERVAL_DEFAULT_VALUE;
import static org.sonatype.central.publisher.plugin.Constants.WAIT_POLLING_INTERVAL_NAME;
import static org.sonatype.central.publisher.plugin.Constants.WAIT_UNTIL_DEFAULT_VALUE;
import static org.sonatype.central.publisher.plugin.Constants.WAIT_UNTIL_NAME;
import static org.sonatype.central.publisher.plugin.deffer.ArtifactDeferrerImpl.INDEX_FILE_NAME;

@Mojo(name = "publish", defaultPhase = LifecyclePhase.DEPLOY, requiresOnline = true, threadSafe = true)
public class PublishMojo
    extends AbstractPublisherMojo
{
  /**
   * Name of the bundle file that the plugin will output as a result.
   *
   * @since 0.1.1
   */
  @Parameter(property = "outputFilename", defaultValue = DEFAULT_BUNDLE_OUTPUT_FILENAME, required = true)
  private String outputFilename;

  /**
   * Name of the directory that the plugin will output the bundle file into.<br>
   * <b>Note:</b> this directory will be cleaned on the first execution of this plugin.
   *
   * @since 0.1.1
   */
  @Parameter(property = "outputDirectory")
  private File forcedOutputDirectory;

  /**
   * Name of the directory where the plugin will stage files.<br>
   * <b>Note:</b> this directory will be cleaned on the first execution of this plugin.
   *
   * @since 0.1.1
   */
  @Parameter(property = "stagingDirectory")
  private File forcedStagingDirectory;

  /**
   * Name of the directory where the plugin will defer snapshot files.<br>
   * <b>Note:</b> this directory will be cleaned on the first execution of this plugin.
   *
   * @since 0.7.0
   */
  @Parameter(property = "deferredDirectory")
  private File forcedDeferredDirectory;

  /**
   * Name of the deployment that's used for uploading and deploying on the central url. If using
   * <code>central.sonatype.com</code>, this is the name that will be visible on the Deployments page.
   *
   * @since 0.1.1
   */
  @Parameter(property = "deploymentName", defaultValue = DEFAULT_DEPLOYMENT_NAME)
  private String deploymentName;

  /**
   * ID of the server that you configured in your <code>settings.xml</code>.
   *
   * @since 0.1.1
   */
  @Parameter(property = PUBLISHING_SERVER_ID_NAME, defaultValue = PUBLISHING_SERVER_ID_DEFAULT_VALUE)
  private String publishingServerId;

  /**
   * Assign whether to auto publish a deployment. Meaning that no manual intervention is required, if a deployment is
   * considered valid, before publishing it. Defaults to {@link Constants#AUTO_PUBLISH_DEFAULT_VALUE}.
   *
   * @since 0.2.0
   */
  @Parameter(property = AUTO_PUBLISH_NAME, defaultValue = AUTO_PUBLISH_DEFAULT_VALUE)
  private boolean autoPublish;

  /**
   * Assign whether to drop a deployment in validated state. Meaning that the deployment is deleted after validation
   * succeeded. This configuration is effective only, if {@link #autoPublish} is not set true. Defaults to
   * {@link Constants#DROP_VALIDATED_DEFAULT_VALUE}.
   *
   * @since 1.0.0
   */
  @Parameter(property = DROP_VALIDATED_NAME, defaultValue = DROP_VALIDATED_DEFAULT_VALUE)
  private boolean dropValidated;

  /**
   * Assign what to wait for, if desired to wait, of the processing of a deployment. See @{@link WaitUntilRequest} for
   * options.
   * <p/>
   * <code>uploaded</code> - Wait until the bundle is being uploaded.
   * <p/>
   * <code>validated</code> - Wait until the uploaded bundle is validated.
   * <p/>
   * <code>published</code> - Wait until the uploaded bundle is published to Maven Central.
   * <p/>
   * Defaults to {@link Constants#WAIT_UNTIL_DEFAULT_VALUE}.
   *
   * @since 0.2.0
   */
  @Parameter(
      property = WAIT_UNTIL_NAME,
      defaultValue = WAIT_UNTIL_DEFAULT_VALUE)
  private String waitUntil;

  /**
   * Assign the amount of seconds that the plugin will wait for a deployment state. Can not be less than
   * {@link Constants#WAIT_MAX_TIME_DEFAULT_VALUE}.
   *
   * @since 0.2.0
   */
  @Parameter(
      property = WAIT_MAX_TIME_NAME,
      defaultValue = WAIT_MAX_TIME_DEFAULT_VALUE)
  private int waitMaxTime;

  /**
   * Assign the amount of seconds between checking whether a deployment has published. Can not be less than
   * {@link Constants#WAIT_POLLING_INTERVAL_DEFAULT_VALUE}.
   *
   * @since 0.2.0
   */
  @Parameter(
      property = WAIT_POLLING_INTERVAL_NAME,
      defaultValue = WAIT_POLLING_INTERVAL_DEFAULT_VALUE)
  private int waitPollingInterval;

  /**
   * @deprecated use {@link #waitPollingInterval} instead
   *
   * @since 0.1.1
   */
  @Deprecated
  @Parameter(
      property = PUBLISH_COMPLETION_POLL_INTERVAL_NAME,
      defaultValue = PUBLISH_COMPLETION_POLL_INTERVAL_DEFAULT_VALUE)
  private int publishCompletionPollInterval;

  /**
   * @deprecated use {@link #autoPublish} in combination with {@link #waitUntil} instead
   *
   * @since 0.1.1
   */
  @Deprecated
  @Parameter(property = WAIT_FOR_PUBLISH_COMPLETION_NAME, defaultValue = WAIT_FOR_PUBLISH_COMPLETION_DEFAULT_VALUE)
  private boolean waitForPublishCompletion;

  /**
   * Assign the URL that this plugin uses to publish releases. Defaults to
   * {@link Constants#CENTRAL_BASE_URL_DEFAULT_VALUE}.
   *
   * @since 0.1.1
   */
  @Parameter(property = CENTRAL_BASE_URL_NAME, defaultValue = CENTRAL_BASE_URL_DEFAULT_VALUE)
  private String centralBaseUrl;

  /**
   * URL that this plugin uses to publish snapshots to. Used by
   * {@link ArtifactDeferrerImpl#getDeploymentRepository(MavenSession, String, String)} as follows:
   * <p/>
   * a) Use this URL if it is set and {@link #publishingServerId} is set.
   * <p/>
   * b) Use repo URL defined by distributionManagement from project POM if this is defined.
   * <p/>
   * c) Use {@link Constants#CENTRAL_SNAPSHOTS_URL_DEFAULT_VALUE} as default if {@link #publishingServerId} is set.
   *
   * @since 0.7.0
   */
  @Parameter(property = CENTRAL_SNAPSHOTS_URL_NAME)
  private String centralSnapshotsUrl;

  /**
   * Assign what type of checksums will be generated for files. Three options are available:
   * <p/>
   * <code>all</code> - Will request MD5, SHA1, SHA256 and SHA512 to be generated.
   * <p/>
   * <code>required</code> - Only MD5 and SHA1 will be requested to be generated.
   * <p/>
   * <code>none</code> - No Checksums will be requested to be generated.
   * <p/>
   *
   * @since 0.1.1
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
   *
   * @since 0.1.6
   */
  @Parameter(property = IGNORE_PUBLISHED_COMPONENTS_NAME, defaultValue = IGNORE_PUBLISHED_COMPONENTS_DEFAULT_VALUE)
  private boolean ignorePublishedComponents;

  /**
   * Assign artifacts that must not be added to the bundle that represents the deployment that will be published.
   *
   * @since 0.1.6
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
  private ArtifactDeferrer artifactDeferrer;

  @Component
  private PublisherClient publisherClient;

  @Component
  private ArtifactUploader artifactUploader;

  @Component
  private DeploymentPublishedWatcher deploymentPublishedWatcher;

  @Component
  private ComponentPublishedChecker componentPublishedChecker;

  @Component
  private DeploymentDeleter deploymentDeleter;

  @Component
  private SettingsDecrypter theCryptKeeper;

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
          WAIT_POLLING_INTERVAL_NAME));

      // only update waitPollingInterval if it was still set to the default
      if (waitPollingInterval == waitPollingIntervalDefault) {
        // convert to seconds from milliseconds
        waitPollingInterval = publishCompletionPollInterval / 1000;
      }
    }

    if (waitPollingInterval < waitPollingIntervalDefault) {
      getLog().warn(format(
          "%s was set to be less than %2$s seconds, will use the default of %2$s seconds.",
          WAIT_POLLING_INTERVAL_NAME,
          WAIT_POLLING_INTERVAL_DEFAULT_VALUE));

      waitPollingInterval = waitPollingIntervalDefault;
    }

    int waitMaxTimeDefault = Integer.parseInt(WAIT_MAX_TIME_DEFAULT_VALUE);
    if (waitMaxTime < waitMaxTimeDefault) {
      getLog().warn(format(
          "%s was set to be less than %2$s seconds, will use the default of %2$s seconds.",
          WAIT_MAX_TIME_NAME,
          WAIT_MAX_TIME_DEFAULT_VALUE));

      waitMaxTime = waitMaxTimeDefault;
    }

    if (!ChecksumRequest.isValidValue(checksums)) {
      throw new MojoExecutionException(format("%s must be one of the following values %s.",
          CHECKSUMS_NAME,
          ChecksumRequest.toNames()));
    }

    checksumRequest = ChecksumRequest.valueOf(checksums.toUpperCase());

    if (!WaitUntilRequest.isValidValue(waitUntil)) {
      throw new MojoExecutionException(format("%s must be one of the following values %s.",
          WAIT_UNTIL_NAME,
          WaitUntilRequest.toNames()));
    }

    waitUntilRequest = WaitUntilRequest.valueOf(waitUntil.toUpperCase());
    publishingType = autoPublish ? PublishingType.AUTOMATIC : PublishingType.USER_MANAGED;

    // waitForPublishCompletion is deprecated, but if used, overwrite actually used values.
    if (waitForPublishCompletion) {
      getLog().warn(format(
          "waitForPublishCompletion is deprecated, using it will set %s to true and %s to %s.",
          AUTO_PUBLISH_NAME,
          WAIT_UNTIL_NAME,
          WaitUntilRequest.PUBLISHED.name().toLowerCase()));

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
          WaitUntilRequest.VALIDATED.name().toLowerCase()));

      waitUntilRequest = WaitUntilRequest.VALIDATED;
    }

    // Disable dropValidated if autoPublish is enabled.
    if (dropValidated && autoPublish) {

      getLog().warn(format(
          "%s is forced to %s since %s is set to %s.",
          DROP_VALIDATED_NAME,
          DROP_VALIDATED_DEFAULT_VALUE,
          AUTO_PUBLISH_NAME,
          Boolean.toString(autoPublish)));

      dropValidated = Boolean.valueOf(DROP_VALIDATED_DEFAULT_VALUE);
    }

    // Are we asked to wait till something different than finished validation while drop validated is enabled? Enforce
    // and log it.
    if (waitUntilRequest != WaitUntilRequest.VALIDATED && dropValidated) {

      getLog().warn(format(
          "Requested to wait for state: %s, but %s is set to %s. Enforced to wait until %s.",
          waitUntilRequest.name().toLowerCase(),
          DROP_VALIDATED_NAME,
          Boolean.toString(dropValidated),
          WaitUntilRequest.VALIDATED.name().toLowerCase()));

      waitUntilRequest = WaitUntilRequest.VALIDATED;
    }
  }

  @Override
  protected void doExecute() throws MojoExecutionException {
    File deferredDirectory = getWorkDirectory(forcedDeferredDirectory, DEFAULT_DEFERRED_DIR_NAME);
    File stagingDirectory = getWorkDirectory(forcedStagingDirectory, DEFAULT_STAGING_DIR_NAME);
    File outputDirectory = getWorkDirectory(forcedOutputDirectory, DEFAULT_BUNDLE_OUTPUT_DIR_NAME);

    List<ArtifactWithFile> artifactWithFiles = getArtifactWithFiles();

    if (getMavenSession().getCurrentProject().getArtifact().isSnapshot()) {
      processSnapshot(artifactWithFiles, deferredDirectory);
    }
    else {
      configurePublisherClient();
      processRelease(artifactWithFiles, stagingDirectory);
    }

    if (isThisLastProjectWithThisMojoInExecution()) {
      MojoExecutionException processSnapshotsFailure = null;

      try {
        postProcessSnapshot(deferredDirectory);
      }
      // Don't fail right away if we can't deploy snapshots, as we still want to deploy releases.
      catch (MojoExecutionException e) {
        processSnapshotsFailure = e;
      }

      try {
        postProcessRelease(stagingDirectory, outputDirectory, deploymentName);
      }
      catch (Exception e) {
        // It might appear as if we are potentially ignoring the snapshot failure if one occurred after a failure
        // occurred during processing releases, but we will log a snapshot failure no matter what. We just throw the
        // release failure right away, treating it as the most important reason for the build failure.
        if (null != processSnapshotsFailure) {
          getLog().error("Failed to deploy deferred artifacts", processSnapshotsFailure);
        }

        throw e;
      }

      // if we passed processing a release, and a snapshot failure occurred, throw it now.
      if (null != processSnapshotsFailure) {
        throw processSnapshotsFailure;
      }
    }
  }

  private void postProcessSnapshot(final File deferredDirectory) throws MojoExecutionException {
    try {
      if (!DirectoryUtils.hasFiles(deferredDirectory)) {
        getLog().debug("Skipping Central Staging Publishing as no staged artifacts were found.");
        return;
      }

      if (Files.exists(new File(deferredDirectory, INDEX_FILE_NAME).toPath())) {

        // note that we pass a null for the remote repository, to get repository from the index from an install.
        artifactDeferrer.deployUp(getMavenSession(), deferredDirectory, null);
      }
      else {
        getLog().debug("Skipping Central SNAPSHOT Publishing as no index with deferred artifacts was found.");
      }
    }
    catch (ArtifactDeploymentException | IOException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  protected void processSnapshot(
      final List<ArtifactWithFile> artifactWithFiles,
      final File deferredDirectory) throws MojoExecutionException
  {
    List<ArtifactWithFile> filteredArtifactWithFiles = artifactWithFiles.stream()
        .filter(artifactWithFile -> {
          if (excludeArtifacts.contains(artifactWithFile.getArtifact().getArtifactId())) {
            return false;
          }

          if (isSkipPublishing()) {
            getLog().info("Skipping Central Snapshot Publishing for artifact '" +
                artifactWithFile.getArtifact().getArtifactId() + "' at user's request.");
            return false;
          }

          return true;
        })
        .collect(toList());

    try {
      artifactDeferrer.install(
          new DeferArtifactRequest(
              getMavenSession(),
              filteredArtifactWithFiles,
              deferredDirectory,
              centralSnapshotsUrl,
              publishingServerId));
    }
    catch (ArtifactInstallationException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  protected void processRelease(
      final List<ArtifactWithFile> artifactWithFiles,
      final File stagingDirectory) throws MojoExecutionException
  {
    List<ArtifactWithFile> filteredArtifactWithFiles = artifactWithFiles.stream()
        .filter(artifactWithFile -> {
          if (excludeArtifacts.contains(artifactWithFile.getArtifact().getArtifactId())) {
            return false;
          }

          if (ignorePublishedComponents) {
            return !componentPublishedChecker.isComponentPublished(artifactWithFile.getArtifact().getGroupId(),
                artifactWithFile.getArtifact().getArtifactId(), artifactWithFile.getArtifact().getVersion());
          }

          if (isSkipPublishing()) {
            getLog().info("Skipping Central Release Publishing for artifact '" +
                artifactWithFile.getArtifact().getArtifactId() + "' at user's request.");
            return false;
          }

          return true;
        })
        .collect(toList());

    try {
      artifactStager.stageArtifact(new StageArtifactRequest(filteredArtifactWithFiles, stagingDirectory));
      artifactBundler.preBundle(getMavenSession().getCurrentProject(), stagingDirectory.toPath(),
          checksumRequest);
    }
    catch (final ArtifactInstallationException e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  protected void postProcessRelease(
      final File stagingDirectory,
      final File outputDirectory,
      final String deploymentName)
  {
    if (!DirectoryUtils.hasFiles(stagingDirectory)) {
      getLog().debug("Skipping Central Release Publishing as no staged artifacts were found.");
      return;
    }

    Path bundleFile = artifactBundler.bundle(
        new BundleArtifactRequest(
            getMavenSession().getCurrentProject(),
            stagingDirectory,
            outputDirectory,
            outputFilename,
            checksumRequest));

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
        waitPollingInterval);

    deploymentPublishedWatcher.waitForDeploymentState(waitForDeploymentStateRequest);

    if (dropValidated) {
      DeleteDeploymentRequest deleteDeploymentRequest = new DeleteDeploymentRequest(deploymentId, deploymentName);

      deploymentDeleter.deleteDeployment(deleteDeploymentRequest);
    }
  }

  protected List<ArtifactWithFile> getArtifactWithFiles() throws MojoExecutionException {
    return unmodifiableList(
        getProjectUtils().getArtifacts(getMavenSession().getCurrentProject(), getArtifactFactory()));
  }

  /**
   * Gets the working directory for the plugin. Defaults to under the target folder unless overridden. Synchronized
   * because the first execution of this plugin cleans the working directory.
   *
   * @param forcedWorkDirectory an override File that points to a valid directory
   * @param relativePath a path relative to the working directory root for the folder
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

  private boolean isThisLastProjectWithThisMojoInExecution() {
    return getMojoUtils().isThisLastProjectWithThisMojoInExecution(
        getMavenSession(),
        getMojoExecution(),
        getPluginGroupId(),
        getPluginArtifactId(),
        isFailOnBuildFailure());
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
    getLog().info("Using Usertoken auth, with namecode: " + authData.getUsername());
    publisherClient.setAuthProvider(USERTOKEN, DEFAULT_ORGANIZATION_ID,
        authData.getUsername(),
        authData.getPassword());
  }

  private AuthData getUserCredentials() {
    try {
      Server server = getMavenSession().getSettings().getServer(publishingServerId);
      if (server == null) {
        throw new IllegalStateException("No <server> with id '" + publishingServerId + "' found in settings.xml");
      }
      SettingsDecryptionResult settingsDecryptionResult =
          theCryptKeeper.decrypt(new DefaultSettingsDecryptionRequest(server));
      Server decrypted = settingsDecryptionResult.getServer();
      if (decrypted == null
          || StringUtils.isBlank(decrypted.getUsername())
          || StringUtils.isBlank(decrypted.getPassword())) {
        if (!settingsDecryptionResult.getProblems().isEmpty()) {
          getLog().warn("Settings decryption problems: " + settingsDecryptionResult.getProblems());
        }
        throw new IllegalStateException("Missing username/password for server id '" + publishingServerId + "'");
      }
      return new AuthData(decrypted.getUsername(), decrypted.getPassword());
    }
    catch (Exception e) {
      throw new RuntimeException("Unable to get publisher server properties for server id: " + publishingServerId,
          e);
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
        centralBaseURL));
  }
}
