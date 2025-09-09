package org.emergent.maven.gitver.core;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.BiFunction;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Tolerate;

@Value
@lombok.Builder(toBuilder = true, setterPrefix = "set", builderClassName = "Builder")
public class GitVerConfig {

  public static final String EXTENSION_DISABLED_ENV_VAR = "GITVER_EXTENSION_DISABLED";
  public static final String EXTENSION_DISABLED_SYSPROP = "gitver.extension.disabled";

  public static final String VERSION_OVERRIDE_ENV_VAR = "GITVER_VERSION_OVERRIDE";
  public static final String VERSION_OVERRIDE_SYSPROP = "gitver.version.override";

  public static final String GV_DISABLED = "gitver.disabled";
  private static final String PROPERTY_PREFIX = "gitver.";
  public static final String GV_INITIAL_MAJOR = "gitver.initial.major";
  public static final String GV_INITIAL_MINOR = "gitver.initial.minor";
  public static final String GV_INITIAL_PATCH = "gitver.initial.patch";
  public static final String GV_KEYWORDS_MAJOR = "gitver.keywords.major";
  public static final String GV_KEYWORDS_MINOR = "gitver.keywords.minor";
  public static final String GV_KEYWORDS_PATCH = "gitver.keywords.patch";
  public static final String GV_KEYWORDS_REGEX = "gitver.keywords.regex";
  public static final String GV_VERSION_PATTERN = "gitver.version.pattern";
  public static final String GV_VERSION_OVERRIDE = "gitver.override";
  public static final String KEY_MAJOR = "[major]";
  public static final String KEY_MINOR = "[minor]";
  public static final String KEY_PATCH = "[patch]";
  public static final String DEFAULT_VERSION_PATTERN = "%M.%m.%p(-%c)(-%S)";

  boolean disabled;

  /**
   * Initial Major version.
   */
  int initialMajor;

  /**
   * Initial Minor version.
   */
  int initialMinor;

  /**
   * Initial Patch version.
   */
  int initialPatch;

  /**
   * The keyword for calculating major version of the SemVer.
   */
  @NonNull
  @lombok.Builder.Default
  String majorKey = KEY_MAJOR;

  /**
   * The keyword for calculating minor version of the SemVer.
   */
  @NonNull
  @lombok.Builder.Default
  String minorKey = KEY_MINOR;

  /**
   * The keyword for calculating patch version of the SemVer.
   */
  @NonNull
  @lombok.Builder.Default
  String patchKey = KEY_PATCH;

  /**
   * Whether to use regex for matching keywords.
   */
  @lombok.Builder.Default
  boolean useRegex = false;

  @NonNull
  @lombok.Builder.Default
  String versionPattern = DEFAULT_VERSION_PATTERN;

  String versionOverride;

  public GitVerConfig(
    boolean disabled,
    int initialMajor, int initialMinor, int initialPatch,
    String majorKey, String minorKey, String patchKey, boolean useRegex,
    String versionPattern,
    String versionOverride) {
    this.disabled = disabled;
    this.initialMajor = Util.assertNotNegative(initialMajor, "InitialVersion.major");
    this.initialMinor = Util.assertNotNegative(initialMinor, "InitialVersion.minor");
    this.initialPatch = Util.assertNotNegative(initialPatch, "InitialVersion.patch");
    this.majorKey = normalize(majorKey).orElse(KEY_MAJOR);
    this.minorKey = normalize(minorKey).orElse(KEY_MINOR);
    this.patchKey = normalize(patchKey).orElse(KEY_PATCH);
    this.useRegex = useRegex;
    this.versionPattern = versionPattern;
    this.versionOverride = versionOverride;
  }

  public Map<String, String> toProperties() {
    Map<String, Object> properties = new TreeMap<>();
    Beeboop b = Beeboop.wrap(properties);
    if (b.put(GV_VERSION_OVERRIDE, getVersionOverride())) {
      return Util.flatten(properties);
    }

    b.apply(GV_VERSION_PATTERN, getVersionPattern())
        .apply(GV_KEYWORDS_MAJOR, getMajorKey())
        .apply(GV_KEYWORDS_MINOR, getMinorKey())
        .apply(GV_KEYWORDS_PATCH, getPatchKey())
        .apply(GV_KEYWORDS_REGEX, isUseRegex());
    return Util.flatten(properties);
  }

  private static boolean isEmpty(Object o) {
    return o == null || o.toString().isEmpty();
  }

  private static boolean nonEmpty(Object o) {
    return !isEmpty(o);
  }

  private interface Beeboop extends BiFunction<String, Object, Beeboop> {

    boolean put(String key, Object val);

    static Beeboop wrap(Map<String, Object> props) {
      return new Beeboop() {
        @Override
        public Beeboop apply(String key, Object val) {
          put(key, val);
          return this;
        }

        @Override
        public boolean put(String key, Object val) {
          Optional<String> opt = Optional.ofNullable(val)
            .map(String::valueOf)
            .filter(v -> !v.isEmpty());
          opt.ifPresent(v -> props.put(key, v));
          return opt.isPresent();
        }
      };
    }
  }

  public static GitVerConfig from(Properties props) {
    return builder()
      .setDisabled(Boolean.parseBoolean(props.getProperty(GV_DISABLED)))
      .setInitialMajor(props.getProperty(GV_INITIAL_MAJOR, "0"))
      .setInitialMinor(props.getProperty(GV_INITIAL_MINOR, "0"))
      .setInitialPatch(props.getProperty(GV_INITIAL_PATCH, "0"))
      .setMajorKey(props.getProperty(GV_KEYWORDS_MAJOR, KEY_MAJOR))
      .setMinorKey(props.getProperty(GV_KEYWORDS_MINOR, KEY_MINOR))
      .setPatchKey(props.getProperty(GV_KEYWORDS_PATCH, KEY_PATCH))
      .setUseRegex(Boolean.parseBoolean(props.getProperty(GV_KEYWORDS_REGEX)))
      .setVersionPattern(props.getProperty(GV_VERSION_PATTERN, DEFAULT_VERSION_PATTERN))
      .setVersionOverride(props.getProperty(GV_VERSION_OVERRIDE, ""))
      .build();
  }

  private static Optional<String> normalize(String value) {
    return Optional.ofNullable(value).filter(v -> !v.isBlank());
  }

  public static GitVerConfig create() {
    return builder().build();
  }

  public static class Builder {

    @Tolerate
    public Builder setInitialMajor(String value) {
      return setInitialMajor(Integer.parseInt(value));
    }

    @Tolerate
    public Builder setInitialMinor(String value) {
      return setInitialMinor(Integer.parseInt(value));
    }

    @Tolerate
    public Builder setInitialPatch(String value) {
      return setInitialPatch(Integer.parseInt(value));
    }

    @Tolerate
    public Builder setUseRegexKey(String value) {
      return setUseRegex(Boolean.parseBoolean(value));
    }
  }
}

