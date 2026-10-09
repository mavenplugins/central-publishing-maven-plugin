/*
 * Copyright (c) 2022-present Sonatype, Inc. All rights reserved.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package org.sonatype.central.publisher.plugin.utils;

import java.nio.file.Path;
import java.util.List;

import org.sonatype.central.publisher.plugin.model.ArtifactWithFile;
import org.sonatype.central.publisher.plugin.model.ChecksumRequest;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

public interface ProjectUtils
{
  List<ArtifactWithFile> getArtifacts(
      final MavenProject mavenProject,
      final ArtifactFactory artifactFactory) throws MojoExecutionException;

  /**
   * Delete {@link ProjectUtilsImpl#MAVEN_METADATA_CENTRAL_STAGING_XML} from the Group and Artifact ID directory of a
   * {@link MavenProject} within the given {@code sourceDir} If the {@link MavenProject} has a parent and is a module,
   * the parent and its child modules will have the xml removed as well.
   *
   * @param project - {@link MavenProject}
   * @param sourceDir - {@link Path}
   */
  void deleteGroupArtifactMavenMetadataCentralStagingXml(final MavenProject project, final Path sourceDir);

  /**
   * Delete the files a local repository keeps about its content from anywhere within the given {@code sourceDir}:
   * {@link ProjectUtilsImpl#MAVEN_METADATA_LOCAL_XML}, {@link ProjectUtilsImpl#REMOTE_REPOSITORIES}, their checksums,
   * and the {@link ProjectUtilsImpl#LOCKS_DIRECTORY}. Since Maven 3.10 staging writes them, and Central rejects a
   * bundle
   * which holds them.
   *
   * @param sourceDir - {@link Path}
   */
  void deleteLocalRepositoryFiles(final Path sourceDir);

  /**
   * Create checksum files for the direct files in the {@code sourceDir}. For example, if a {@code sourceDir} contains
   * the file TEST-1.0.pom, checksum files will be created for all the requested checksums in the given
   * {@code checksumRequest}, like TEST-1.0.pom.md5
   * <p>
   * This method doesn't recursively go through child folders.
   *
   * @param project - {@link MavenProject}
   * @param sourceDir - {@link Path}
   * @param checksumRequest - {@link ChecksumRequest}
   */
  void createChecksumFiles(final MavenProject project, final Path sourceDir, final ChecksumRequest checksumRequest);
}
