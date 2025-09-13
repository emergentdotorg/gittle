package org.emergent.maven.gitver.core;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.emergent.maven.gitver.core.version.VersionStrategy;

public class Util {

  public static final String DISABLED_ENV_VAR = "GV_EXTENSION_DISABLED";
  public static final String DISABLED_SYSPROP = "gv.extension.disabled";

  public static final String GITVER_POM_XML = ".gitver.pom.xml";

  public static final String GITVER_EXTENSION_PROPERTIES = "gitver-maven-extension.properties";

  public static final String GITVER_PROPERTIES = "gitver.properties";

  public static final String VERSION_REGEX_STRING =
    "^(refs/tags/)?(?<tag>v?(?<version>(?<major>[0-9]+)\\.(?<minor>[0-9]+)\\.(?<patch>[0-9]+)))$";

  public static final Pattern VERSION_REGEX = Pattern.compile(VERSION_REGEX_STRING);

  public static final Pattern VERSION_REGEX2 = Pattern.compile(
    "^(?<version>(?<major>[0-9]+)\\.(?<minor>[0-9]+)\\.(?<patch>[0-9]+)(-(?<prerelease>([1-9][0-9]*|[0-9A-Za-z-]+)))?(\\+(?<buildmeta>[0-9A-Za-z-]+))?)$");

  public static final String DOT_MVN = ".mvn";

  public static boolean isDisabled() {
    return Stream.of(System.getProperty(DISABLED_SYSPROP), System.getenv(DISABLED_ENV_VAR))
      .filter(Util::isNotEmpty).findFirst().map(Boolean::parseBoolean).orElse(false);
  }

  public static Path getDotMvnDir(Path currentDir) {
    Path refDir = currentDir.toAbsolutePath();
    while (refDir != null && !Files.exists(refDir.resolve(DOT_MVN))) {
      refDir = refDir.getParent();
    }
    return Optional.ofNullable(refDir).map(r -> r.resolve(DOT_MVN)).orElse(currentDir);
  }

  public static GitverConfig loadConfig(Path currentDir) {
    Path extConfigFile = Util.getExtensionPropsFile(currentDir);
    Properties extensionProps = Util.loadPropsFromFile(extConfigFile);
    return GitverConfig.from(extensionProps);
  }

  public static Path getExtensionPropsFile(Path currentDir) {
    return getDotMvnDir(currentDir).resolve(GITVER_EXTENSION_PROPERTIES);
  }

  public static Properties loadPropsFromFile(Path propertiesPath) {
    Properties props = new Properties();
    if (propertiesPath.toFile().exists()) {
      try (Reader reader = Files.newBufferedReader(propertiesPath)) {
        props.load(reader);
      } catch (IOException e) {
        throw new GitverException("Failed to load properties file " + propertiesPath.toString(), e);
      }
    }
    return props;
  }


  public static String toShortHash(String hash) {
    return hash != null ? hash.substring(0, 8) : null;
  }

  public static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  public static boolean isNotBlank(String value) {
    return !isBlank(value);
  }

  public static boolean isEmpty(String value) {
    return value == null || value.isEmpty();
  }

  public static boolean isNotEmpty(String value) {
    return !isEmpty(value);
  }

  public static void closeQuietly(Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (IOException ignored) {
      }
    }
  }

  public static <T> T mustPass(T value, Predicate<T> condition, Supplier<String> message) {
    if (!condition.test(value)) {
      throw new IllegalArgumentException(message.get());
    }
    return value;
  }

  public static int assertNotNegative(Integer value) {
    return assertNotNegative(value, "Number " + value);
  }

  public static int assertNotNegative(Integer value, String label) {
    return mustPass(
      value, v -> v >= 0, () -> java.lang.String.format("%s must be a non-negative integer", label));
  }

  public static <T> T assertNotNull(T value) {
    if (value == null) throw new NullPointerException();
    return value;
  }

  public static void check(boolean condition) {
    if (!condition) throw new IllegalStateException();
  }

  public static void check(boolean condition, String message) {
    if (!condition) throw new IllegalStateException(message);
  }

  public static Map<String, String> flatten(Properties properties) {
    return properties.entrySet().stream()
      .collect(Collectors.toMap(
        e -> String.valueOf(e.getKey()),
        e -> String.valueOf(e.getValue()),
        (u, v) -> { throw new IllegalStateException("Duplicate key"); },
        TreeMap::new
      ));
  }

  public static Map<String, String> flatten(Map<String, ?> properties) {
    return properties.entrySet().stream()
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        e -> String.valueOf(e.getValue()),
        (u, v) -> { throw new IllegalStateException("Duplicate key"); },
        TreeMap::new
      ));
  }

  public static Properties toProperties(Map<String, Object> properties) {
    Map<String, String> flattened = properties.entrySet().stream()
      .collect(Collectors.toMap(Map.Entry::getKey, e -> String.valueOf(e.getValue())));
    Properties props = new Properties();
    props.putAll(flattened);
    return props;
  }

  public static Map<String, String> toProperties(VersionStrategy versionStrategy) {
    return versionStrategy.toProperties();
  }

  public static Coordinates getCoreCoordinates() {
    try (InputStream is = Util.class.getResourceAsStream(GITVER_PROPERTIES)) {
      Properties props = new Properties();
      props.load(is);
      return Coordinates.builder()
        .setGroupId(props.getProperty("projectGroupId"))
        .setArtifactId(props.getProperty("projectArtifactId"))
        .setVersion(props.getProperty("projectVersion"))
        .build();
    } catch (Exception e) {
      throw new GitverException(e.getMessage(), e);
    }
  }

  public static Coordinates getExtensionCoordinates() {
    Coordinates core = getCoreCoordinates();
    return core.toBuilder().setArtifactId(core.getArtifactId().replace("-core", "-extension")).build();
  }

  public static Coordinates getPluginCoordinates() {
    Coordinates core = getCoreCoordinates();
    return core.toBuilder().setArtifactId(core.getArtifactId().replace("-core", "-plugin")).build();
  }

  public static void go(Map<String, String> map, String key, boolean val) {
    map.put(key, Boolean.toString(val));
  }

  public static void go(Map<String, String> map, String key, int nbr) {
    if (nbr != 0) map.put(key, Integer.toString(nbr));
  }

  public static void go(Map<String, String> map, String key, String val) {
    if (isNotEmpty(val)) map.put(key, val);
  }

}
