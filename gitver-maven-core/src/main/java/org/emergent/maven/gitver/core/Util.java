package org.emergent.maven.gitver.core;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;

public class Util {

    public static final String DISABLED_ENV_VAR = "GV_EXTENSION_DISABLED";
    public static final String DISABLED_SYSPROP = "gv.extension.disabled";

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
        return PropCodec.getInstance().toStringStringMap(properties);
    }

    public static Map<String, String> flatten(Map<String, ?> map) {
        return PropCodec.getInstance().toStringStringMap(map);
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

    public static String join(Map<String, ?> properties) {
        return join(properties, new StringJoiner("\n", "\n---\n", "\n---").setEmptyValue(""))
                .toString();
    }

    public static StringJoiner join(Properties properties, StringJoiner joiner) {
        return join(flatten(properties), joiner);
    }

    public static StringJoiner join(Map<String, ?> properties, StringJoiner joiner) {
        properties.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).forEach(joiner::add);
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

    public static String substringAfter(String str, String separator) {
        return StringUtils.substringAfter(str, separator);
    }

    public static boolean startsWith(String str, String prefix) {
        return Strings.CS.startsWith(str, prefix);
    }

    public static Map<String, String> toStringStringMap(Properties properties) {
        return toStringStringMap(properties.entrySet());
    }

    public static Map<String, String> toStringStringMap(Map<?, ?> map) {
        return toStringStringMap(map.entrySet());
    }

    private static Map<String, String> toStringStringMap(Set<? extends Entry<?, ?>> entries) {
        return entries.stream()
          .filter(e -> e.getKey() instanceof String && e.getValue() instanceof String)
          .collect(CollectorsEx.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue())));
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
