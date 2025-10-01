package org.emergent.maven.gitver.core;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.emergent.maven.gitver.core.version.OverrideStrategy;
import org.emergent.maven.gitver.core.version.PatternStrategy;
import org.emergent.maven.gitver.core.version.ResolvedData;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serial;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Util {

    public static final String DISABLED_ENV_VAR = "GV_EXTENSION_DISABLED";
    public static final String DISABLED_SYSPROP = "gittle.extension.disabled";

    public static final String GITVER_POM_XML = ".gitver.pom.xml";

    public static final String GITVER_EXTENSION_PROPERTIES = "gitver-maven-extension.properties";

    public static final String GITVER_PROPERTIES = "gitver.properties";

    public static final Pattern UBER_REGEX = Pattern.compile("^" + "(?<prefix>refs/tags/)?"
            + "(?<version>"
            + "(?<major>[0-9]+)\\.(?<minor>[0-9]+)\\.(?<patch>[0-9]+)"
            + "(-(?<prerelease>([1-9][0-9]*|[0-9A-Za-z-]+)))?"
            + "(\\+(?<buildmeta>[0-9A-Za-z-]+))?"
            + ")$");

    public static final String VERSION_REGEX_STRING =
            "^(refs/tags/)?(?<tag>v?(?<version>(?<major>[0-9]+)\\.(?<minor>[0-9]+)\\.(?<patch>[0-9]+)))$";

    public static final Pattern VERSION_REGEX = Pattern.compile(VERSION_REGEX_STRING);

    public static final Pattern VERSION_REGEX2 = Pattern.compile(
            "^(?<version>(?<major>[0-9]+)\\.(?<minor>[0-9]+)\\.(?<patch>[0-9]+)(-(?<prerelease>([1-9][0-9]*|[0-9A-Za-z-]+)))?(\\+(?<buildmeta>[0-9A-Za-z-]+))?)$");

    public static final String DOT_MVN = ".mvn";

    public static boolean isDisabled() {
        return Stream.of(System.getProperty(DISABLED_SYSPROP), System.getenv(DISABLED_ENV_VAR))
                .filter(Util::isNotEmpty)
                .findFirst()
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    public static Path getDotMvnDir(Path currentDir) {
        Path refDir = currentDir.toAbsolutePath();
        while (refDir != null && !Files.exists(refDir.resolve(DOT_MVN))) {
            refDir = refDir.getParent();
        }
        return Optional.ofNullable(refDir).map(r -> r.resolve(DOT_MVN)).orElse(currentDir);
    }

    public static Properties loadProperties(Path currentDir) {
        Path extConfigFile = getExtensionPropsFile(currentDir);
        return Util.loadPropsFromFile(extConfigFile);
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
        return mustPass(value, v -> v >= 0, () -> java.lang.String.format("%s must be a non-negative integer", label));
    }

    public static <T> T assertNotNull(T value) {
        if (value == null) {
            throw new NullPointerException();
        }
        return value;
    }

    public static void check(boolean condition) {
        if (!condition) {
            throw new IllegalStateException();
        }
    }

    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    public static Map<String, String> flatten(Properties properties) {
        return toStringStringMap(properties);
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
        return core.toBuilder()
                .setArtifactId(core.getArtifactId().replace("-core", "-extension"))
                .build();
    }

    public static Coordinates getPluginCoordinates() {
        Coordinates core = getCoreCoordinates();
        return core.toBuilder()
                .setArtifactId(core.getArtifactId().replace("-core", "-plugin"))
                .build();
    }

    public static String join(Properties properties) {
        return join(flatten(properties));
    }

    public static String join(Map<String, String> properties) {
        return join(properties, new StringJoiner("\n", "\n---\n", "\n---").setEmptyValue(""))
                .toString();
    }

    private static StringJoiner join(Properties properties, StringJoiner joiner) {
        return join(flatten(properties), joiner);
    }

    private static StringJoiner join(Map<String, String> properties, StringJoiner joiner) {
        properties.forEach((k, v) -> joiner.add(k + "=" + v));
//        properties.entrySet().stream()
//                .map(e -> e.getKey() + "=" + e.getValue())
//                .forEachOrdered(joiner::add);
        return joiner;
    }

    public static <T> Supplier<T> memoize(Supplier<T> delegate) {
        return (delegate instanceof MemoizingSupplier)
                ? delegate
                : new MemoizingSupplier<>(Objects.requireNonNull(delegate));
    }

    public static String replacePrefix(String str, String old, String neo) {
        return neo + substringAfter(str, old);
    }

    public static String substringBefore(String str, String separator) {
        return StringUtils.substringBefore(str, separator);
    }

    public static String substringAfter(String str, String separator) {
        return StringUtils.substringAfter(str, separator);
    }

    public static boolean startsWith(String str, String prefix) {
        return Strings.CS.startsWith(str, prefix);
    }

    public static <K,V> Map<V, K> getReversed(Map<K, V> map) {
        return map.entrySet().stream()
                .collect(CollectorsEx.toLinkedHashMap(Entry::getValue, Entry::getKey));
    }

    public static Map<String, String> toStringStringMap(Properties properties) {
        return properties.stringPropertyNames().stream().collect(Collectors.toMap(n -> n, properties::getProperty));
    }

    public static Properties toProperties(Map<String, String> map) {
        Properties props = new Properties();
        map.forEach(props::setProperty);
        return props;
    }

    private static void log(Map<String, Object> outMap, Type typeOfSrc) {
        System.out.printf(
                "%s properties: %s%n",
                typeOfSrc.getTypeName(),
                outMap.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("\n", "\n", "\n")));
    }

    public static <V> Map<String, V> appendPrefix(String prefix, Map<String, V> props) {
        return props.entrySet().stream().map(e -> appendPrefix(prefix, e)).collect(CollectorsEx.toLinkedHashMap());
    }

    public static <V> Map.Entry<String, V> appendPrefix(String prefix, Map.Entry<String, V> e) {
        return Map.entry(prefix + e.getKey(), e.getValue());
    }

    public static <V> Map<String, V> removePrefix(String prefix, Map<String, V> props) {
        return removePrefixInline(prefix, new LinkedHashMap<>(props));
    }

    public static <V> Map<String, V> removePrefixInline(String prefix, Map<String, V> props) {
        return replacePrefixInline(props, prefix, "");
    }

    public static <V> Map<String, V> replacePrefixInline(Map<String, V> props, String pre, String rep) {
        props.keySet().stream().toList().stream()
                .filter(k -> Util.startsWith(k, pre))
                .forEachOrdered(k -> props.put(rep + substringAfter(k, pre), props.remove(k)));
        return props;
    }

    public static GitverConfig newGitverConfig(Map<String, String> props) {
        return PropCodec.toGitverConfig(FlatProperties.from(props));
    }

    public static GitverConfig newGitverConfig(Path currentDir) {
        Properties extensionProps = loadProperties(currentDir);
        return newGitverConfig(toStringStringMap(extensionProps));
    }

    public static ResolvedData newResolvedData(Map<String, String> props) {
        return PropCodec.toResolvedData(FlatProperties.from(props));
    }

    public static PatternStrategy newPatternStrategy(Map<String, String> props) {
        return PropCodec.toPatternStrategy(FlatProperties.from(props));
    }

    public static OverrideStrategy newOverrideStrategy(Map<String, String> props) {
        return PropCodec.toOverrideStrategy(FlatProperties.from(props));
    }

    private static class MemoizingSupplier<T> implements Supplier<T>, Serializable {

        @Serial
        private static final long serialVersionUID = 0;

        private final Supplier<T> delegate;
        private transient volatile boolean initialized;
        // "value" does not need to be volatile; visibility piggy-backs on volatile read of "initialized".
        private transient T value;

        private MemoizingSupplier(Supplier<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public T get() {
            // A 2-field variant of Double Checked Locking.
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        T t = delegate.get();
                        value = t;
                        initialized = true;
                        return t;
                    }
                }
            }
            return value;
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "(" + delegate + ")";
        }
    }
}
