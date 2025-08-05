/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.model;

import java.io.File;

import org.apache.maven.artifact.Artifact;

public class ArtifactWithFile
{
  private final File file;

  private final Artifact artifact;

  public ArtifactWithFile(final File file, final Artifact artifact) {
    this.file = file;
    this.artifact = artifact;
  }

  public File getFile() {
    return file;
  }

  public Artifact getArtifact() {
    return artifact;
  }
}
