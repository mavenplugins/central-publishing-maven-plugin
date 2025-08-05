/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.bundler;

import java.io.File;
import java.nio.file.Path;

import org.sonatype.central.publisher.client.PublisherClientFactory;
import org.sonatype.central.publisher.client.httpclient.utils.PublisherBundle;
import org.sonatype.central.publisher.plugin.model.BundleArtifactRequest;
import org.sonatype.central.publisher.plugin.model.ChecksumRequest;
import org.sonatype.central.publisher.plugin.utils.ProjectUtils;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.AbstractLogEnabled;

@Component(role = ArtifactBundler.class)
public class ArtifactBundlerImpl
    extends AbstractLogEnabled
    implements ArtifactBundler
{
  @Requirement
  private ProjectUtils projectUtils;

  @Override
  public Path bundle(final BundleArtifactRequest bundleArtifactRequest) {
    File bundleFile = new File(bundleArtifactRequest.getOutputDirectory(), bundleArtifactRequest.getOutputFilename());
    getLogger().info("Going to create " + bundleFile.getAbsolutePath() + " by bundling content at " +
        bundleArtifactRequest.getStagingDirectory().getAbsolutePath());

    Path sourceDir = bundleArtifactRequest.getStagingDirectory().toPath();
    Path destDir = bundleArtifactRequest.getOutputDirectory().toPath();

    PublisherBundle publisherBundle = PublisherClientFactory
        .createPublisherClient()
        .compose(sourceDir, destDir, bundleArtifactRequest.getOutputFilename());

    getLogger().info("Created bundle successfully " + new File(bundleArtifactRequest.getStagingDirectory(),
        bundleArtifactRequest.getOutputFilename()).getAbsolutePath());

    return publisherBundle.getPath();
  }

  @Override
  public void preBundle(final MavenProject project, final Path sourceDir, final ChecksumRequest checksumRequest) {
    projectUtils.deleteGroupArtifactMavenMetadataCentralStagingXml(project, sourceDir);
    projectUtils.createChecksumFiles(project, sourceDir, checksumRequest);
  }
}
