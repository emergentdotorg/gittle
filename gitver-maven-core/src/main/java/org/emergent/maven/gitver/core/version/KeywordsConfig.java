package org.emergent.maven.gitver.core.version;

import java.util.Map;
import java.util.Properties;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.emergent.maven.gitver.core.Mapper;

import static org.emergent.maven.gitver.core.Constants.DEFAULT_MAJOR_KEYWORD;
import static org.emergent.maven.gitver.core.Constants.DEFAULT_MINOR_KEYWORD;
import static org.emergent.maven.gitver.core.Constants.DEFAULT_PATCH_KEYWORD;
import static org.emergent.maven.gitver.core.Constants.GV_KEYWORDS_MAJOR;
import static org.emergent.maven.gitver.core.Constants.GV_KEYWORDS_MINOR;
import static org.emergent.maven.gitver.core.Constants.GV_KEYWORDS_PATCH;
import static org.emergent.maven.gitver.core.Constants.GV_KEYWORDS_REGEX;

@Value
@Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class KeywordsConfig {

  @NonNull
  @lombok.Builder.Default
  String majorKeywords = DEFAULT_MAJOR_KEYWORD;
  @NonNull
  @lombok.Builder.Default
  String minorKeywords = DEFAULT_MINOR_KEYWORD;
  @NonNull
  @lombok.Builder.Default
  String patchKeywords = DEFAULT_PATCH_KEYWORD;
  boolean regexKeywords;

  public static KeywordsConfig from(Properties p) {
    return KeywordsConfig.builder()
      .setMajorKeywords(p.getProperty(GV_KEYWORDS_MAJOR, DEFAULT_MAJOR_KEYWORD))
      .setMinorKeywords(p.getProperty(GV_KEYWORDS_MINOR, DEFAULT_MINOR_KEYWORD))
      .setPatchKeywords(p.getProperty(GV_KEYWORDS_PATCH, DEFAULT_PATCH_KEYWORD))
      .setRegexKeywords(Boolean.parseBoolean(p.getProperty(GV_KEYWORDS_REGEX)))
      .build();
  }

  public Map<String, String> toProperties() {
    Mapper m = Mapper.create()
      .put(GV_KEYWORDS_MAJOR, getMajorKeywords(), DEFAULT_MAJOR_KEYWORD)
      .put(GV_KEYWORDS_MINOR, getMinorKeywords(), DEFAULT_MINOR_KEYWORD)
      .put(GV_KEYWORDS_PATCH, getPatchKeywords(), DEFAULT_PATCH_KEYWORD)
      .put(GV_KEYWORDS_REGEX, isRegexKeywords());
    return m.toMap();
  }
}
