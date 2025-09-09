package org.emergent.maven.gitver.extension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.emergent.maven.gitver.core.ArtifactCoordinates;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.version.VersionStrategy;

import static org.emergent.maven.gitver.core.GitVerConfig.EXTENSION_DISABLED_ENV_VAR;
import static org.emergent.maven.gitver.core.GitVerConfig.EXTENSION_DISABLED_SYSPROP;

class Util {

  public static final String GITVER_EXTENSION_PROPERTIES = "gitver-maven-extension.properties";
  public static final String DOT_MVN = ".mvn";
  public static final String GITVER_POM_XML = ".gitver.pom.xml";
  public static final String GITVER_PROPERTIES = "gitver.properties";

  private Util() {}

  public static boolean isDisabled() {
    return Stream.of(System.getProperty(EXTENSION_DISABLED_SYSPROP), System.getenv(EXTENSION_DISABLED_ENV_VAR))
      .filter(Util::isNotBlank).findFirst().map(Boolean::valueOf).orElse(false);
  }

  public static Path resolveGitVerPom(File basedir) {
    return resolveGitVerPom(basedir.toPath());
  }

  public static Path resolveGitVerPom(Path basedir) {
    return basedir.resolve(GITVER_POM_XML);
  }

  public static Path writePom(Model projectModel, Path originalPomPath) {
    Path newPomPath = originalPomPath.resolveSibling(GITVER_POM_XML);
    writePomx(projectModel, newPomPath);
    return newPomPath;
  }

  public static boolean writePomx(Model projectModel, Path newPomPath) {
    try (Writer fileWriter = Files.newBufferedWriter(newPomPath, Charset.defaultCharset())) {
      MavenXpp3Writer writer = new MavenXpp3Writer();
      writer.write(fileWriter, projectModel);
    } catch (IOException e) {
      throw new GitverException(e.getMessage(), e);
    }
    return Files.exists(newPomPath);
  }

  public static Model readPom(Path pomPath) {
    try (InputStream inputStream = Files.newInputStream(pomPath)) {
      MavenXpp3Reader reader = new MavenXpp3Reader();
      return reader.read(inputStream);
    } catch (IOException | XmlPullParserException e) {
      throw new GitverException(e.getMessage(), e);
    }
  }

  public static ArtifactCoordinates extensionArtifact() {
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

  public static Map<String, String> flatten(Map<String, ?> properties) {
    return org.emergent.maven.gitver.core.Util.flatten(properties);
  }

  public static Map<String, String> toProperties(VersionStrategy versionStrategy) {
    return versionStrategy.toProperties();
  }

  public static boolean isBlank(String s) {
    return org.emergent.maven.gitver.core.Util.isBlank(s);
  }

  public static boolean isNotBlank(String s) {
    return org.emergent.maven.gitver.core.Util.isNotBlank(s);
  }

  public static boolean isEmpty(String s) {
    return org.emergent.maven.gitver.core.Util.isEmpty(s);
  }

  public static boolean isNotEmpty(String s) {
    return org.emergent.maven.gitver.core.Util.isNotEmpty(s);
  }
}
