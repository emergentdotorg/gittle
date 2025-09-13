package org.emergent.maven.gitver.core;

import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.emergent.maven.gitver.core.version.BasicVersion;
import org.emergent.maven.gitver.core.version.KeywordsConfig;

import static org.emergent.maven.gitver.core.Constants.GV_VERSION_INITIAL;
import static org.emergent.maven.gitver.core.Constants.GV_VERSION_OVERRIDE;
import static org.emergent.maven.gitver.core.Constants.GV_VERSION_PATTERN;
import static org.emergent.maven.gitver.core.Constants.VERSION_INITAL_DEF;
import static org.emergent.maven.gitver.core.Constants.VERSION_PATTERN_DEF;

@Value
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@lombok.Builder(toBuilder = true, setterPrefix = "set", builderClassName = "Builder")
public class GitverConfig {

  @NonNull
  @lombok.Builder.Default
  String versionInitial = VERSION_INITAL_DEF;

  @NonNull
  @lombok.Builder.Default
  String versionOverride = "";

  @NonNull
  @lombok.Builder.Default
  String versionPattern = VERSION_PATTERN_DEF;

  @NonNull
  @lombok.Builder.Default
  KeywordsConfig keywords = KeywordsConfig.DEFAULT_KEYWORDS;

  public static GitverConfig from(Properties props) {
    return builder()
      .setVersionInitial(props.getProperty(GV_VERSION_INITIAL, VERSION_INITAL_DEF))
      .setVersionOverride(props.getProperty(GV_VERSION_OVERRIDE, ""))
      .setVersionPattern(props.getProperty(GV_VERSION_PATTERN, VERSION_PATTERN_DEF))
      .setKeywords(KeywordsConfig.from(props))
      .build();
  }

  public Map<String, String> toProperties() {
    Mapper m = Mapper.create()
      .put(GV_VERSION_INITIAL, getVersionInitial(), VERSION_INITAL_DEF)
      .put(GV_VERSION_OVERRIDE, getVersionOverride(), "")
      .put(GV_VERSION_PATTERN, getVersionPattern(), VERSION_PATTERN_DEF)
      .putAll(keywords.toProperties());
    return m.toMap();
  }

  public BasicVersion getInitial() {
    Matcher m = Util.VERSION_REGEX.matcher(versionInitial);
    if (!m.matches()) {
      log.error("Invalid initial version string: " + versionInitial);
      throw new IllegalArgumentException("Invalid version initial string: " + versionInitial);
    }
    BasicVersion initParsed = BasicVersion.builder()
      .setMajor(m.group("major"))
      .setMinor(m.group("minor"))
      .setPatch(m.group("patch"))
      .build();
    log.warn("Parsed initial version string: " + initParsed);
    return initParsed;
  }
}

