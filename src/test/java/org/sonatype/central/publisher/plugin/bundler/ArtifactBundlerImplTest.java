package org.sonatype.central.publisher.plugin.bundler;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.sonatype.central.publisher.plugin.model.ChecksumRequest;
import org.sonatype.central.publisher.plugin.utils.ProjectUtils;

import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.inOrder;

public class ArtifactBundlerImplTest
{
  @Mock
  private ProjectUtils projectUtils;

  @InjectMocks
  private ArtifactBundlerImpl artifactBundler;

  @Before
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  /**
   * The local repository files go before the checksums are made: _remote.repositories sits among the artifacts of a
   * version, and would get checksums of its own.
   */
  @Test
  public void deletesTheLocalRepositoryFilesBeforeTheChecksumsAreMade() {
    MavenProject project = new MavenProject();
    Path stagingDir = Paths.get("target", "central-staging");
    ChecksumRequest checksumRequest = ChecksumRequest.REQUIRED;

    artifactBundler.preBundle(project, stagingDir, checksumRequest);

    InOrder order = inOrder(projectUtils);
    order.verify(projectUtils).deleteLocalRepositoryFiles(stagingDir);
    order.verify(projectUtils).createChecksumFiles(project, stagingDir, checksumRequest);
  }
}
