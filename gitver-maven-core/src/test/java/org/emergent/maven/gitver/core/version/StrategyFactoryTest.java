package org.emergent.maven.gitver.core.version;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class StrategyFactoryTest {

  @ParameterizedTest
  @CsvSource({
    "Fix: [minor] corrected typo, .*\\[minor\\].*, true",
    "[minor] Fix: corrected typo, ^\\[minor\\].*, true",
    "Fix: [minor] corrected typo, ^\\[minor\\].*, false",
    "Update: improved performance, [minor], false"
  })
  public void testHasValueWithRegex(String commitMessage, String keyword, boolean expected) {
    KeywordsConfig versionConfig = KeywordsConfig.builder()
      .setRegex(true)
      .build();

    Assertions.assertThat(StrategyFactory.hasValue(versionConfig, commitMessage, keyword)).isEqualTo(expected);
  }

  @Test
  public void testHasValueWithoutRegex() {
    KeywordsConfig versionConfig = KeywordsConfig.builder()
      .setRegex(false)
      .build();

    String commitMessage = "Fix: [minor] corrected typo";
    assertThat(StrategyFactory.hasValue(versionConfig, commitMessage, "[minor]")).isTrue();

    commitMessage = "Update: improved performance";
    assertThat(StrategyFactory.hasValue(versionConfig, commitMessage, "[minor]")).isFalse();
  }
}
