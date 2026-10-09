package org.sonatype.central.publisher.plugin.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.codehaus.plexus.logging.Logger;
import org.codehaus.plexus.logging.console.ConsoleLogger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

/**
 * Tests that the files a local repository keeps about its content are removed from the staging directory.
 *
 * Maven 3.10 stages the artifacts with a Resolver 2 local repository manager, which writes maven-metadata-local.xml,
 * _remote.repositories and a .locks directory next to them. Central rejects a bundle which holds them: "Bundle has
 * content that does NOT have a .pom file".
 */
public class ProjectUtilsImplLocalRepositoryFilesTest
{
  private static final String GROUP_ARTIFACT = "io/github/test/test-app";

  private static final String VERSION = GROUP_ARTIFACT + "/1.0.0";

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private ProjectUtilsImpl projectUtils;

  private Path stagingDir;

  @Before
  public void setUp() throws IOException {
    projectUtils = new ProjectUtilsImpl();
    projectUtils.enableLogging(new ConsoleLogger(Logger.LEVEL_DISABLED, "test"));
    stagingDir = tempDir.newFolder("central-staging").toPath();
  }

  /**
   * The staging directory as Maven 3.10.0 leaves it for a jar project.
   */
  @Test
  public void deletesTheFilesMaven310Writes() throws IOException {
    createArtifacts(VERSION, "test-app-1.0.0");
    create(GROUP_ARTIFACT + "/maven-metadata-local.xml");
    create(VERSION + "/_remote.repositories");
    create(".locks/artifact~io.github.test~test-app~1.0.0.lock");

    projectUtils.deleteLocalRepositoryFiles(stagingDir);

    assertThat(files(), containsInAnyOrder(artifacts(VERSION, "test-app-1.0.0")));
    assertThat(Files.exists(stagingDir.resolve(".locks")), is(false));
  }

  @Test
  public void deletesTheChecksumsOfThoseFiles() throws IOException {
    createArtifacts(VERSION, "test-app-1.0.0");
    create(GROUP_ARTIFACT + "/maven-metadata-local.xml");
    create(GROUP_ARTIFACT + "/maven-metadata-local.xml.md5");
    create(GROUP_ARTIFACT + "/maven-metadata-local.xml.sha1");
    create(VERSION + "/_remote.repositories");
    create(VERSION + "/_remote.repositories.sha256");

    projectUtils.deleteLocalRepositoryFiles(stagingDir);

    assertThat(files(), containsInAnyOrder(artifacts(VERSION, "test-app-1.0.0")));
  }

  /**
   * Every module of a multi-module build is staged into the same directory.
   */
  @Test
  public void deletesThemForEveryModule() throws IOException {
    createArtifacts("io/github/test/parent/1.0.0", "parent-1.0.0");
    createArtifacts("io/github/test/module/1.0.0", "module-1.0.0");
    create("io/github/test/parent/maven-metadata-local.xml");
    create("io/github/test/parent/1.0.0/_remote.repositories");
    create("io/github/test/module/maven-metadata-local.xml");
    create("io/github/test/module/1.0.0/_remote.repositories");

    projectUtils.deleteLocalRepositoryFiles(stagingDir);

    assertThat(files(), containsInAnyOrder(Stream.concat(
        Stream.of(artifacts("io/github/test/parent/1.0.0", "parent-1.0.0")),
        Stream.of(artifacts("io/github/test/module/1.0.0", "module-1.0.0"))).toArray(String[]::new)));
  }

  /**
   * The staging directory as Maven 3.9 leaves it once the plugin deleted maven-metadata-central-staging.xml.
   */
  @Test
  public void keepsAStagingDirectoryWithoutThem() throws IOException {
    createArtifacts(VERSION, "test-app-1.0.0");

    projectUtils.deleteLocalRepositoryFiles(stagingDir);

    assertThat(files(), containsInAnyOrder(artifacts(VERSION, "test-app-1.0.0")));
  }

  @Test
  public void keepsAnArtifactWhoseNameOnlyStartsLikeOne() throws IOException {
    createArtifacts(VERSION, "test-app-1.0.0");
    create(VERSION + "/maven-metadata-local.xml-notes.txt");

    projectUtils.deleteLocalRepositoryFiles(stagingDir);

    assertThat(files().contains(VERSION + "/maven-metadata-local.xml-notes.txt"), is(true));
  }

  @Test
  public void ignoresAStagingDirectoryWhichDoesNotExist() {
    projectUtils.deleteLocalRepositoryFiles(stagingDir.resolve("missing"));
  }

  private void createArtifacts(final String dir, final String baseName) throws IOException {
    for (String name : artifacts(dir, baseName)) {
      create(name);
    }
  }

  private static String[] artifacts(final String dir, final String baseName) {
    return new String[]{
        dir + "/" + baseName + ".pom",
        dir + "/" + baseName + ".pom.asc",
        dir + "/" + baseName + ".jar",
        dir + "/" + baseName + ".jar.asc"
    };
  }

  private void create(final String relativePath) throws IOException {
    Path file = stagingDir.resolve(relativePath);
    Files.createDirectories(file.getParent());
    Files.write(file, relativePath.getBytes());
  }

  private List<String> files() throws IOException {
    try (Stream<Path> paths = Files.walk(stagingDir)) {
      return paths.filter(Files::isRegularFile)
          .map(path -> stagingDir.relativize(path).toString().replace('\\', '/'))
          .collect(toList());
    }
  }
}
