package org.emergent.maven.gitver.core;

import java.io.Closeable;
import java.io.IOException;
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
import org.emergent.maven.gitver.core.version.VersionStrategy;

public final class Util {

  public static final String VERSION_REGEX_STRING =
    "^(refs/tags/)?(?<tag>v?(?<version>(?<major>[0-9]+)\\.(?<minor>[0-9]+)\\.(?<patch>[0-9]+)))$";

  public static final Pattern VERSION_REGEX = Pattern.compile(VERSION_REGEX_STRING);

  public static final Pattern VERSION_REGEX2 = Pattern.compile(
    "^(?<version>(?<major>[0-9]+)\\.(?<minor>[0-9]+)\\.(?<patch>[0-9]+)(-(?<prerelease>([1-9][0-9]*|[0-9A-Za-z-]+)))?(\\+(?<buildmeta>[0-9A-Za-z-]+))?)$");

  public static final String DOT_MVN = ".mvn";

  private Util() {}

  public static Path getDOTMVNDirectory(Path currentDir) {
    Path refDir = currentDir;
    while (refDir != null && !Files.exists(refDir.resolve(DOT_MVN))) {
      refDir = refDir.getParent();
    }
    return Optional.ofNullable(refDir).map(r -> r.resolve(DOT_MVN)).orElse(currentDir);
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
}
