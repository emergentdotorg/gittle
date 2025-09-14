package org.emergent.maven.gitver.core.version;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class KeywordsConfigTest {

  @Test
  public void setKeywordValues() {
    KeywordsConfig config = KeywordsConfig.builder()
        .setMajor("[TEST1]")
        .setMinor("[TEST2]")
        .setPatch("[TEST3]")
        .setRegex(true)
        .build();
    assertThat(config)
      .extracting("major", "minor", "patch", "regex")
      .containsExactly("[TEST1]", "[TEST2]", "[TEST3]", true);
  }

}
