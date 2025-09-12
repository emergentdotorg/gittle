package org.emergent.maven.gitver.core;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
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
  String majorKeywords = KEY_MAJOR;

  /**
   * The keyword for calculating minor version of the SemVer.
   */
  @NonNull
  @lombok.Builder.Default
  String minorKeywords = KEY_MINOR;

  /**
   * The keyword for calculating patch version of the SemVer.
   */
  @NonNull
  @lombok.Builder.Default
  String patchKeywords = KEY_PATCH;

  /**
   * Whether to use regex for matching keywords.
   */
  @lombok.Builder.Default
  boolean regexKeywords = false;

  @NonNull
  @lombok.Builder.Default
  String versionPattern = DEFAULT_VERSION_PATTERN;

  @NonNull
  @lombok.Builder.Default
  String versionOverride = "";

  {
    validate();
  }

  private void validate() {
    Util.assertNotNegative(initialMajor, GV_INITIAL_MAJOR);
    Util.assertNotNegative(initialMinor, GV_INITIAL_MINOR);
    Util.assertNotNegative(initialPatch, GV_INITIAL_PATCH);
  }

  public Map<String, String> toProperties() {
    Map<String, Object> properties = new TreeMap<>();
    properties.put(GV_DISABLED, isDisabled());
    properties.put(GV_VERSION_PATTERN, getVersionPattern());
    properties.put(GV_VERSION_OVERRIDE, getVersionOverride());
    properties.put(GV_INITIAL_MAJOR, getInitialMajor());
    properties.put(GV_INITIAL_MINOR, getInitialMinor());
    properties.put(GV_INITIAL_PATCH, getInitialPatch());
    properties.put(GV_KEYWORDS_MAJOR, getMajorKeywords());
    properties.put(GV_KEYWORDS_MINOR, getMinorKeywords());
    properties.put(GV_KEYWORDS_PATCH, getPatchKeywords());
    properties.put(GV_KEYWORDS_REGEX, isRegexKeywords());
    return Util.flatten(properties);
  }

  public static VersionConfig from(Properties props) {
    return builder()
      .setDisabled(Boolean.parseBoolean(props.getProperty(GV_DISABLED)))
      .setInitialMajor(getInt(props, GV_INITIAL_MAJOR))
      .setInitialMinor(getInt(props, GV_INITIAL_MINOR))
      .setInitialPatch(getInt(props, GV_INITIAL_PATCH))
      .setMajorKeywords(props.getProperty(GV_KEYWORDS_MAJOR, KEY_MAJOR))
      .setMinorKeywords(props.getProperty(GV_KEYWORDS_MINOR, KEY_MINOR))
      .setPatchKeywords(props.getProperty(GV_KEYWORDS_PATCH, KEY_PATCH))
      .setRegexKeywords(Boolean.parseBoolean(props.getProperty(GV_KEYWORDS_REGEX)))
      .setVersionPattern(props.getProperty(GV_VERSION_PATTERN, DEFAULT_VERSION_PATTERN))
      .setVersionOverride(props.getProperty(GV_VERSION_OVERRIDE, ""))
      .build();
  }

  public List<String> getMajorKeywordsList() {
    return Arrays.asList(majorKeywords.split(","));
  }

  public List<String> getMinorKeywordsList() {
    return Arrays.asList(minorKeywords.split(","));
  }

  public List<String> getPatchKeywordsList() {
    return Arrays.asList(patchKeywords.split(","));
  }

  private static int getInt(Properties props, String key) {
    return Integer.parseInt(props.getProperty(key, "0"));
  }

  private static Optional<String> normalize(String value) {
    return Optional.ofNullable(value).filter(v -> !v.isBlank());
  }

  public static class Builder {

    public Builder setInitialMajor(int initialMajor) {
      this.initialMajor = Util.assertNotNegative(initialMajor, GV_INITIAL_MAJOR);;
      return this;
    }

    public Builder setInitialMinor(int initialMinor) {
      this.initialMinor = Util.assertNotNegative(initialMinor, GV_INITIAL_MINOR);;
      return this;
    }

    public Builder setInitialPatch(int initialPatch) {
      this.initialPatch = Util.assertNotNegative(initialPatch, GV_INITIAL_PATCH);;
      return this;
    }
  }
}

