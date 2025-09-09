package org.emergent.maven.gitver.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.emergent.maven.gitver.core.ArtifactCoordinates;
import org.emergent.maven.gitver.core.GitVerConfig;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.git.GitUtil;
import org.emergent.maven.gitver.core.version.VersionStrategy;

class Util {

  public static final String GITVER_POM_XML = ".gitver.pom.xml";
  public static final String GITVER_PROPERTIES = "gitver.properties";
  public static final String GITVER_EXTENSION_PROPERTIES = "gitver-maven-extension.properties";
  private static final String DOT_MVN = ".mvn";

  private Util() {}

  public static Path getNewPom(MavenProject mavenProject) {
    Path basedir = mavenProject.getBasedir().toPath();
    //Path targetdir = basedir.resolve(mavenProject.getBuild().getDirectory());
    return basedir.resolve(GITVER_POM_XML);
  }

  public static void writePom(Model projectModel, Path newPom) {
    try {
      Files.createDirectories(newPom.getParent());
      try (Writer fileWriter = Files.newBufferedWriter(newPom)) {
        MavenXpp3Writer writer = new MavenXpp3Writer();
        writer.write(fileWriter, projectModel);
      }
    } catch (IOException e) {
      throw new GitverException(e.getMessage(), e);
    }
  }

  public static ArtifactCoordinates pluginArtifact() {
    try (InputStream is = Util.class.getResourceAsStream(GITVER_PROPERTIES)) {
      Properties props = new Properties();
      props.load(is);
      return ArtifactCoordinates.builder()
        .setGroupId(props.getProperty("projectGroupId"))
        .setArtifactId(props.getProperty("projectArtifactId"))
        .setVersion(props.getProperty("projectVersion"))
        .build();
    } catch (Exception e) {
      throw new GitverException(e.getMessage(), e);
    }
  }

  static VersionStrategy getVersionStrategy(MavenProject mavenProject) {
    Path projectDirectory = mavenProject.getBasedir().toPath();
//    File projectDirectory = mavenProject.getModel().getProjectDirectory().toPath();
    Path dotmvnDirectory = getDOTMVNDirectory(projectDirectory).orElse(projectDirectory);
    Properties fileProps = loadExtensionProperties(dotmvnDirectory);
    GitVerConfig config = GitVerConfig.from(fileProps);
    return GitUtil.getVersionStrategy(projectDirectory, config);
  }

  private static Properties loadExtensionProperties(Path dotmvnDirectory) {
    Properties props = new Properties();
    Path propertiesPath = dotmvnDirectory.resolve(GITVER_EXTENSION_PROPERTIES);
    if (Files.exists(propertiesPath)) {
      try (Reader reader = Files.newBufferedReader(propertiesPath)) {
        props.load(reader);
      } catch (IOException e) {
        throw new GitverException("Failed to load extensions properties file", e);
      }
    }
    return props;
  }

  private static Optional<Path> getDOTMVNDirectory(Path currentDir) {
    Path refDir = currentDir;
    while (refDir != null && !Files.exists(refDir.resolve(DOT_MVN))) {
      refDir = refDir.getParent();
    }
    return Optional.ofNullable(refDir).map(r -> r.resolve(DOT_MVN));
  }
}
