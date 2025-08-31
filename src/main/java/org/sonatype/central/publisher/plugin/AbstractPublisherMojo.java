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
   * For creating only the bundle but skipping uploading and publishing. This is useful for creating a bundle and
   * manually uploading it through <code>central.sonatype.com</code>.
   *
   * @since 0.1.1
   */
  @Parameter(property = "skipPublishing", defaultValue = "false", required = true)
  private boolean skipPublishing;

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
    return skipPublishing;
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
