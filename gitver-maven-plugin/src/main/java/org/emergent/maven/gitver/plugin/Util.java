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
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.VersionConfig;

class Util {

  public static final String GITVER_POM_XML = ".gitver.pom.xml";
  public static final String GITVER_PROPERTIES = "gitver.properties";
  private static final String GITVER_EXTENSION_PROPERTIES = "gitver-maven-extension.properties";
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

  private VersionConfig loadConfig(Path dotmvnDirectory) {
    Properties fileProps = loadExtensionProperties(dotmvnDirectory);
    Properties fqFileProps = new Properties();
    fileProps.forEach((key, value) -> {
      String k = String.valueOf(key);
      if (!k.startsWith("gitver.")) {
        k = "gitver." + k;
      }
      fqFileProps.put(k, value);
    });
    Properties props = new Properties(fqFileProps);
    System.getenv().forEach((k, v) -> {
      if (k.startsWith("GITVER_")) {
        String propKey = k.replace("GITVER_", "gitver.").replaceAll("_", ".");
        props.setProperty(propKey, v);
      }
    });
    props.putAll(System.getProperties());
    return VersionConfig.from(props);
  }

  private Properties loadExtensionProperties(Path dotmvnDirectory) {
    Properties props = new Properties();
    Path propertiesPath = dotmvnDirectory.resolve(GITVER_EXTENSION_PROPERTIES);
    if (propertiesPath.toFile().exists()) {
      //LOGGER.debug("Reading gitver properties from {}", propertiesPath);
      try (Reader reader = Files.newBufferedReader(propertiesPath)) {
        props.load(reader);
      } catch (IOException e) {
        throw new GitverException("Failed to load extensions properties file", e);
      }
    }
    return props;
  }

  private static Optional<Path> getDOTMVNDirectory(Path currentDir) {
    //LOGGER.info("Finding .mvn in {}", currentDir);
    Path refDir = currentDir;
    while (refDir != null && !Files.exists(refDir.resolve(DOT_MVN))) {
      refDir = refDir.getParent();
    }
    return Optional.ofNullable(refDir).map(r -> r.resolve(DOT_MVN));
  }

}
