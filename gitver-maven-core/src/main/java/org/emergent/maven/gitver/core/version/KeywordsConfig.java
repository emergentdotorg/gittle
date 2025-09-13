package org.emergent.maven.gitver.core.version;

import java.util.Map;
import java.util.Properties;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.emergent.maven.gitver.core.Mapper;

import static org.emergent.maven.gitver.core.Constants.GV_KEYWORDS_MAJOR;
import static org.emergent.maven.gitver.core.Constants.GV_KEYWORDS_MINOR;
import static org.emergent.maven.gitver.core.Constants.GV_KEYWORDS_PATCH;
import static org.emergent.maven.gitver.core.Constants.GV_KEYWORDS_REGEX;

@Value
@Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class KeywordsConfig {

  public static final KeywordsConfig DEFAULT_KEYWORDS = KeywordsConfig.builder().build();

  public static final String MAJOR_KEYWORD_DEF = "[major]";
  public static final String MINOR_KEYWORD_DEF = "[minor]";
  public static final String PATCH_KEYWORD_DEF = "[patch]";

  @NonNull
  @lombok.Builder.Default
  String major = MAJOR_KEYWORD_DEF;

  @NonNull
  @lombok.Builder.Default
  String minor = MINOR_KEYWORD_DEF;

  @NonNull
  @lombok.Builder.Default
  String patch = PATCH_KEYWORD_DEF;

  boolean regex;

  public static KeywordsConfig from(Properties p) {
    return KeywordsConfig.builder()
      .setMajor(p.getProperty(GV_KEYWORDS_MAJOR, MAJOR_KEYWORD_DEF))
      .setMinor(p.getProperty(GV_KEYWORDS_MINOR, MINOR_KEYWORD_DEF))
      .setPatch(p.getProperty(GV_KEYWORDS_PATCH, PATCH_KEYWORD_DEF))
      .setRegex(Boolean.parseBoolean(p.getProperty(GV_KEYWORDS_REGEX)))
      .build();
  }

  public Map<String, String> toProperties() {
    Mapper m = Mapper.create()
      .put(GV_KEYWORDS_MAJOR, getMajor(), MAJOR_KEYWORD_DEF)
      .put(GV_KEYWORDS_MINOR, getMinor(), MINOR_KEYWORD_DEF)
      .put(GV_KEYWORDS_PATCH, getPatch(), PATCH_KEYWORD_DEF)
      .put(GV_KEYWORDS_REGEX, isRegex());
    return m.toMap();
  }
}
