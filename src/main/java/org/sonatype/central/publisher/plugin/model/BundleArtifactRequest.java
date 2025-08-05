/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.model;

import java.io.File;

import org.apache.maven.project.MavenProject;

public class BundleArtifactRequest
{
  private final MavenProject project;

  private final File stagingDirectory;

  private final File outputDirectory;

  private final String outputFilename;

  private final ChecksumRequest checksumRequest;

  public BundleArtifactRequest(
      final MavenProject project,
      final File stagingDirectory,
      final File outputDirectory,
      final String outputFilename,
      final ChecksumRequest checksumRequest
  )
  {
    this.project = project;
    this.stagingDirectory = stagingDirectory;
    this.outputDirectory = outputDirectory;
    this.outputFilename = outputFilename;
    this.checksumRequest = checksumRequest;
  }

  public MavenProject getProject() {
    return project;
  }

  public File getOutputDirectory() {
    return outputDirectory;
  }

  public File getStagingDirectory() {
    return stagingDirectory;
  }

  public String getOutputFilename() {
    return outputFilename;
  }

  public ChecksumRequest getChecksumRequest() {
    return checksumRequest;
  }
}
