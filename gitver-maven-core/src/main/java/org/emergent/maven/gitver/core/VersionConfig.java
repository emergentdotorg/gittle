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
  String majorKeywords = Constants.DEFAULT_MAJOR_KEYWORD;

  /**
   * The keyword for calculating minor version of the SemVer.
   */
  @NonNull
  @lombok.Builder.Default
  String minorKeywords = Constants.DEFAULT_MINOR_KEYWORD;

  /**
   * The keyword for calculating patch version of the SemVer.
   */
  @NonNull
  @lombok.Builder.Default
  String patchKeywords = Constants.DEFAULT_PATCH_KEYWORD;

  /**
   * Whether to use regex for matching keywords.
   */
  @lombok.Builder.Default
  boolean regexKeywords = false;

  @NonNull
  @lombok.Builder.Default
  String versionPattern = Constants.DEFAULT_VERSION_PATTERN;

  @NonNull
  @lombok.Builder.Default
  String versionOverride = "";

  {
    validate();
  }

  private void validate() {
    Util.assertNotNegative(initialMajor, Constants.GV_INITIAL_MAJOR);
    Util.assertNotNegative(initialMinor, Constants.GV_INITIAL_MINOR);
    Util.assertNotNegative(initialPatch, Constants.GV_INITIAL_PATCH);
  }

  public Map<String, String> toProperties() {
    Map<String, Object> properties = new TreeMap<>();
    properties.put(Constants.GV_DISABLED, isDisabled());
    properties.put(Constants.GV_VERSION_PATTERN, getVersionPattern());
    properties.put(Constants.GV_VERSION_OVERRIDE, getVersionOverride());
    properties.put(Constants.GV_INITIAL_MAJOR, getInitialMajor());
    properties.put(Constants.GV_INITIAL_MINOR, getInitialMinor());
    properties.put(Constants.GV_INITIAL_PATCH, getInitialPatch());
    properties.put(Constants.GV_KEYWORDS_MAJOR, getMajorKeywords());
    properties.put(Constants.GV_KEYWORDS_MINOR, getMinorKeywords());
    properties.put(Constants.GV_KEYWORDS_PATCH, getPatchKeywords());
    properties.put(Constants.GV_KEYWORDS_REGEX, isRegexKeywords());
    return Util.flatten(properties);
  }

  public static VersionConfig from(Properties props) {
    return builder()
      .setDisabled(Boolean.parseBoolean(props.getProperty(Constants.GV_DISABLED)))
      .setInitialMajor(getInt(props, Constants.GV_INITIAL_MAJOR))
      .setInitialMinor(getInt(props, Constants.GV_INITIAL_MINOR))
      .setInitialPatch(getInt(props, Constants.GV_INITIAL_PATCH))
      .setMajorKeywords(props.getProperty(Constants.GV_KEYWORDS_MAJOR, Constants.DEFAULT_MAJOR_KEYWORD))
      .setMinorKeywords(props.getProperty(Constants.GV_KEYWORDS_MINOR, Constants.DEFAULT_MINOR_KEYWORD))
      .setPatchKeywords(props.getProperty(Constants.GV_KEYWORDS_PATCH, Constants.DEFAULT_PATCH_KEYWORD))
      .setRegexKeywords(Boolean.parseBoolean(props.getProperty(Constants.GV_KEYWORDS_REGEX)))
      .setVersionPattern(props.getProperty(Constants.GV_VERSION_PATTERN, Constants.DEFAULT_VERSION_PATTERN))
      .setVersionOverride(props.getProperty(Constants.GV_VERSION_OVERRIDE, ""))
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
      this.initialMajor = Util.assertNotNegative(initialMajor, Constants.GV_INITIAL_MAJOR);;
      return this;
    }

    public Builder setInitialMinor(int initialMinor) {
      this.initialMinor = Util.assertNotNegative(initialMinor, Constants.GV_INITIAL_MINOR);;
      return this;
    }

    public Builder setInitialPatch(int initialPatch) {
      this.initialPatch = Util.assertNotNegative(initialPatch, Constants.GV_INITIAL_PATCH);;
      return this;
    }
  }
}

