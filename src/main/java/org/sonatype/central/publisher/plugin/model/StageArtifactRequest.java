/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.model;

import java.io.File;
import java.util.List;

public class StageArtifactRequest
{
  private final List<ArtifactWithFile> artifactWithFiles;

  private final File stagingDirectory;

  public StageArtifactRequest(final List<ArtifactWithFile> artifactWithFiles, final File stagingDirectory) {
    this.artifactWithFiles = artifactWithFiles;
    this.stagingDirectory = stagingDirectory;
  }

  public List<ArtifactWithFile> getArtifactWithFiles() {
    return artifactWithFiles;
  }

  public File getStagingDirectory() {
    return stagingDirectory;
  }
}
