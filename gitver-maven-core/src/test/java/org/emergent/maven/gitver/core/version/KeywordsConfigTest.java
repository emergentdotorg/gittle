package org.emergent.maven.gitver.core.version;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KeywordsConfigTest {

  @Test
  public void setKeywordValues() {
    KeywordsConfig config = KeywordsConfig.builder()
        .setMajorKeywords("[TEST1]")
        .setMinorKeywords("[TEST2]")
        .setPatchKeywords("[TEST3]")
        .setRegexKeywords(true)
        .build();
    assertThat(config)
      .extracting("majorKeywords", "minorKeywords", "patchKeywords", "regexKeywords")
      .containsExactly("[TEST1]", "[TEST2]", "[TEST3]", true);
  }

}
