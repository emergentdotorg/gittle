package org.emergent.maven.gitver.core;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Tolerate;

@Value
@lombok.Builder(toBuilder = true, setterPrefix = "set", builderClassName = "Builder")
public class VersionConfig {

  private static final String PROPERTY_INBOUND_PREFIX = "gv.";
  private static final String PROPERTY_OUTBOUND_PREFIX = "gitver.";

  public static final String GV_DISABLED = "gv.disabled";
  public static final String GV_INITIAL_MAJOR = "gv.initial.major";
  public static final String GV_INITIAL_MINOR = "gv.initial.minor";
  public static final String GV_INITIAL_PATCH = "gv.initial.patch";
  public static final String GV_KEYWORDS_MAJOR = "gv.keywords.major";
  public static final String GV_KEYWORDS_MINOR = "gv.keywords.minor";
  public static final String GV_KEYWORDS_PATCH = "gv.keywords.patch";
  public static final String GV_KEYWORDS_REGEX = "gv.keywords.regex";
  public static final String GV_VERSION_PATTERN = "gv.version.pattern";
  public static final String GV_VERSION_OVERRIDE = "gv.version.override";

  public static final String GITVER_VERSION = "gitver.version";

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

  @NonNull
  @lombok.Builder.Default
  String versionOverride = "";

  public VersionConfig(
    boolean disabled,
    int initialMajor, int initialMinor, int initialPatch,
    String majorKey, String minorKey, String patchKey, boolean useRegex,
    String versionPattern,
    String versionOverride) {
    this.disabled = disabled;
    this.initialMajor = Util.assertNotNegative(initialMajor, GV_INITIAL_MAJOR);
    this.initialMinor = Util.assertNotNegative(initialMinor, GV_INITIAL_MINOR);
    this.initialPatch = Util.assertNotNegative(initialPatch, GV_INITIAL_PATCH);
    this.majorKey = normalize(majorKey).orElse(KEY_MAJOR);
    this.minorKey = normalize(minorKey).orElse(KEY_MINOR);
    this.patchKey = normalize(patchKey).orElse(KEY_PATCH);
    this.useRegex = useRegex;
    this.versionPattern = versionPattern;
    this.versionOverride = versionOverride;
  }

  public Map<String, String> toProperties() {
    Map<String, Object> properties = new TreeMap<>();
    properties.put(GV_DISABLED, isDisabled());
    properties.put(GV_VERSION_PATTERN, getVersionPattern());
    properties.put(GV_VERSION_OVERRIDE, getVersionOverride());
    properties.put(GV_INITIAL_MAJOR, getInitialMajor());
    properties.put(GV_INITIAL_MINOR, getInitialMinor());
    properties.put(GV_INITIAL_PATCH, getInitialPatch());
    properties.put(GV_KEYWORDS_MAJOR, getMajorKey());
    properties.put(GV_KEYWORDS_MINOR, getMinorKey());
    properties.put(GV_KEYWORDS_PATCH, getPatchKey());
    properties.put(GV_KEYWORDS_REGEX, isUseRegex());
    return Util.flatten(properties);
  }

  public static VersionConfig from(Properties props) {
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

  public static VersionConfig create() {
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

