package org.sonatype.central.publisher.plugin.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import org.sonatype.central.publisher.plugin.model.ArtifactWithFile;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * Tests that Maven 4 consumer POM is used as the main POM when staging artifacts.
 *
 * Maven 4 generates two POMs:
 * - build POM (mavenProject.getFile()) - contains ${revision}, model 4.1.0, build config
 * - consumer POM (attached artifact, classifier="consumer", type="pom") - resolved, model 4.0.0
 *
 * Central Portal requires the consumer POM as the main .pom file.
 */
public class ProjectUtilsImplConsumerPomTest
{

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private ProjectUtilsImpl projectUtils;

  private ArtifactFactory artifactFactory;

  private File buildPomFile;

  private File consumerPomFile;

  private File jarFile;

  @Before
  public void setUp() throws IOException {
    projectUtils = new ProjectUtilsImpl();
    artifactFactory = mock(ArtifactFactory.class);

    buildPomFile = tempDir.newFile("pom.xml");
    Files.write(buildPomFile.toPath(), Arrays.asList(
        "<project xmlns=\"http://maven.apache.org/POM/4.1.0\">",
        "  <modelVersion>4.1.0</modelVersion>",
        "  <groupId>io.github.test</groupId>",
        "  <artifactId>test-app</artifactId>",
        "  <version>${revision}</version>",
        "</project>"));

    consumerPomFile = tempDir.newFile("consumer.pom");
    Files.write(consumerPomFile.toPath(), Arrays.asList(
        "<project xmlns=\"http://maven.apache.org/POM/4.0.0\">",
        "  <modelVersion>4.0.0</modelVersion>",
        "  <groupId>io.github.test</groupId>",
        "  <artifactId>test-app</artifactId>",
        "  <version>1.0.0</version>",
        "</project>"));

    jarFile = tempDir.newFile("test-app-1.0.0.jar");
  }

  /**
   * When Maven 4 attaches a consumer POM (classifier="consumer", type="pom"),
   * the plugin should NOT include it as a separate artifact in the bundle.
   * Verifies consumer POM is used as main POM and filtered from artifacts.
   */
  @Test
  public void shouldNotIncludeConsumerPomAsSeparateArtifact() throws Exception {
    MavenProject project = createProject();

    Artifact mainArtifact = createArtifact(null, "jar");
    mainArtifact.setFile(jarFile);
    project.setArtifact(mainArtifact);

    // Maven 4 attaches consumer POM with classifier "consumer"
    Artifact consumerPom = createArtifact("consumer", "pom");
    consumerPom.setFile(consumerPomFile);

    // GPG plugin creates signature for consumer POM (type "pom.asc")
    File consumerPomAscFile = tempDir.newFile("consumer.pom.asc");
    Artifact consumerPomAsc = createArtifact("consumer", "pom.asc");
    consumerPomAsc.setFile(consumerPomAscFile);

    // GPG plugin creates signature for build POM (type "pom.asc", no classifier)
    File buildPomAscFile = tempDir.newFile("build.pom.asc");
    Artifact buildPomAsc = createArtifact(null, "pom.asc");
    buildPomAsc.setFile(buildPomAscFile);

    File sourcesFile = tempDir.newFile("test-app-1.0.0-sources.jar");
    Artifact sources = createArtifact("sources", "jar");
    sources.setFile(sourcesFile);

    project.addAttachedArtifact(consumerPom);
    project.addAttachedArtifact(consumerPomAsc);
    project.addAttachedArtifact(buildPomAsc);
    project.addAttachedArtifact(sources);

    List<ArtifactWithFile> result = projectUtils.getArtifacts(project, artifactFactory);

    // Consumer POM should NOT appear as separate artifact
    long consumerPomCount = result.stream()
        .filter(a -> consumerPomFile.equals(a.getFile()))
        .count();
    assertThat("Consumer POM should not be a separate artifact in the bundle",
        consumerPomCount, is(0L));

    // Build POM signature should be replaced with consumer POM signature
    long buildPomAscCount = result.stream()
        .filter(a -> buildPomAscFile.equals(a.getFile()))
        .count();
    assertThat("Build POM signature should not be in the bundle",
        buildPomAscCount, is(0L));

    long consumerPomAscCount = result.stream()
        .filter(a -> consumerPomAscFile.equals(a.getFile()))
        .count();
    assertThat("Consumer POM signature should replace build POM signature",
        consumerPomAscCount, is(1L));

    // Should have: JAR + pom.asc (swapped) + sources = 3
    assertThat(result, hasSize(3));
  }

  /**
   * Without consumer POM (Maven 3 project), behavior should be unchanged.
   */
  @Test
  public void shouldWorkNormallyWithoutConsumerPom() throws Exception {
    MavenProject project = createProject();

    Artifact mainArtifact = createArtifact(null, "jar");
    mainArtifact.setFile(jarFile);
    project.setArtifact(mainArtifact);

    File sourcesFile = tempDir.newFile("sources.jar");
    Artifact sources = createArtifact("sources", "jar");
    sources.setFile(sourcesFile);

    project.addAttachedArtifact(sources);

    List<ArtifactWithFile> result = projectUtils.getArtifacts(project, artifactFactory);

    assertThat(result, hasSize(2));
  }

  private MavenProject createProject() {
    Model model = new Model();
    model.setGroupId("io.github.test");
    model.setArtifactId("test-app");
    model.setVersion("1.0.0");
    model.setPackaging("jar");
    MavenProject project = new MavenProject(model);
    project.setFile(buildPomFile);
    return project;
  }

  private Artifact createArtifact(String classifier, String type) {
    return new DefaultArtifact(
        "io.github.test", "test-app", "1.0.0",
        null, type, classifier, new DefaultArtifactHandler(type));
  }
}
