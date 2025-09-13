package org.emergent.maven.gitver.core;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import org.emergent.maven.gitver.core.version.BasicVersion;
import org.emergent.maven.gitver.core.version.KeywordsConfig;

import static org.emergent.maven.gitver.core.Constants.DEFAULT_VERSION_PATTERN;
import static org.emergent.maven.gitver.core.Constants.GV_DISABLED;
import static org.emergent.maven.gitver.core.Constants.GV_VERSION_OVERRIDE;
import static org.emergent.maven.gitver.core.Constants.GV_VERSION_PATTERN;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@lombok.Builder(toBuilder = true, setterPrefix = "set", builderClassName = "Builder")
public class GitverConfig {

  boolean disabled;

  @NonNull
  @lombok.Builder.Default
  BasicVersion initial = BasicVersion.builder().build();

  @NonNull
  @lombok.Builder.Default
  KeywordsConfig keywords = KeywordsConfig.from(new Properties());

  @NonNull
  @lombok.Builder.Default
  String versionPattern = DEFAULT_VERSION_PATTERN;

  @NonNull
  @lombok.Builder.Default
  String versionOverride = "";

  public Map<String, String> toProperties() {
    Mapper m = Mapper.create()
      .put(GV_DISABLED, isDisabled())
      .put(GV_VERSION_PATTERN, getVersionPattern(), DEFAULT_VERSION_PATTERN)
      .put(GV_VERSION_OVERRIDE, getVersionOverride(), "")
      .putAll(getInitial().toProperties("gv.initial."))
      .putAll(keywords.toProperties());
    return m.toMap();
  }

  public static GitverConfig from(Properties props) {
    return builder()
      .setDisabled(Boolean.parseBoolean(props.getProperty(GV_DISABLED)))
      .setInitial(BasicVersion.from("gv.initial.", props))
      .setVersionPattern(props.getProperty(GV_VERSION_PATTERN, DEFAULT_VERSION_PATTERN))
      .setVersionOverride(props.getProperty(GV_VERSION_OVERRIDE, ""))
      .setKeywords(KeywordsConfig.from(props))
      .build();
  }

  private static int getInt(Properties props, String key) {
    return Integer.parseInt(props.getProperty(key, "0"));
  }

  private static Optional<String> normalize(String value) {
    return Optional.ofNullable(value).filter(v -> !v.isBlank());
  }

  public static class Builder {

  }
}

