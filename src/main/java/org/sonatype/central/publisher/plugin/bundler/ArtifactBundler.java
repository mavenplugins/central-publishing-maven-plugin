/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.bundler;

import java.nio.file.Path;

import org.sonatype.central.publisher.plugin.model.BundleArtifactRequest;
import org.sonatype.central.publisher.plugin.model.ChecksumRequest;

import org.apache.maven.project.MavenProject;

public interface ArtifactBundler
{
  Path bundle(final BundleArtifactRequest bundleArtifactRequest);

  void preBundle(MavenProject project, Path sourceDir, ChecksumRequest checksumRequest);
}
