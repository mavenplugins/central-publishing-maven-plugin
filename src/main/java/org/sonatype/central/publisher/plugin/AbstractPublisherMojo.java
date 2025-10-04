/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin;

import org.sonatype.central.publisher.plugin.utils.MojoUtils;
import org.sonatype.central.publisher.plugin.utils.ProjectUtils;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractPublisherMojo
    extends AbstractMojo
{
  /**
   * For skipping artifacts bundling and uploading.
   *
   * This is useful for skipping particular or all artifacts from bundling and uploading to
   * <code>central.sonatype.com</code>.<br>
   * Since <code>0.9.0</code> checked per module and artifact.
   *
   * @since 0.1.1
   */
  @Parameter(property = "skipPublishing", defaultValue = "false", required = true)
  private boolean skipPublishing;

  /**
   * For skipping artifacts bundling.
   *
   * This is particularly useful for multi-module build, where some sub-modules should
   * not be included in the final bundle that will be uploaded to <code>central.sonatype.com</code>.
   *
   * @since 1.1.0
   * @deprecated Use <code>skipPublishing</code> since <code>1.2.0</code> instead.
   */
  @Deprecated
  @Parameter(property = "skipBundling", defaultValue = "false", required = false)
  private boolean skipBundling;

  /**
   * Indicates if building is allowed to have the plugin fail before uploading and publishing occurs.
   *
   * @since 0.1.1
   */
  @Parameter(property = "failOnBuildFailure", defaultValue = "true")
  private boolean failOnBuildFailure;

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession mavenSession;

  @Parameter(defaultValue = "${plugin.groupId}", readonly = true, required = true)
  private String pluginGroupId;

  @Parameter(defaultValue = "${plugin.artifactId}", readonly = true, required = true)
  private String pluginArtifactId;

  @Parameter(defaultValue = "${plugin.version}", readonly = true, required = true)
  private String pluginVersion;

  @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
  private MojoExecution mojoExecution;

  @Component
  private ArtifactFactory artifactFactory;

  @Component
  private MojoUtils mojoUtils;

  @Component
  private ProjectUtils projectUtils;

  protected MavenSession getMavenSession() {
    return mavenSession;
  }

  protected MojoExecution getMojoExecution() {
    return mojoExecution;
  }

  protected ArtifactFactory getArtifactFactory() {
    return artifactFactory;
  }

  protected MojoUtils getMojoUtils() {
    return mojoUtils;
  }

  protected ProjectUtils getProjectUtils() {
    return projectUtils;
  }

  protected String getPluginGroupId() {
    return pluginGroupId;
  }

  protected String getPluginArtifactId() {
    return pluginArtifactId;
  }

  protected boolean isFailOnBuildFailure() {
    return failOnBuildFailure;
  }

  protected boolean isSkipPublishing() {
    return skipPublishing || isSkipBundling();
  }

  /**
   * @return {@link #skipBundling}
   * @deprecated Use {@link #isSkipPublishing()} instead.
   */
  @Deprecated
  protected boolean isSkipBundling() {
    return skipBundling;
  }

  protected String getPluginGav() {
    return pluginGroupId + ":" + pluginArtifactId + ":" + pluginVersion;
  }

  @Override
  public void execute() throws MojoExecutionException {
    validateParameters();
    doExecute();
  }

  public void validateParameters() throws MojoExecutionException {
    doValidateParameters();
  }

  protected abstract void doExecute() throws MojoExecutionException;

  protected abstract void doValidateParameters() throws MojoExecutionException;
}
