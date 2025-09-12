package org.emergent.maven.gitver.core.git;

import org.emergent.maven.gitver.core.VersionConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class GitUtilTest {

  @ParameterizedTest
  @CsvSource({
    "Fix: [minor] corrected typo, .*\\[minor\\].*, true",
    "[minor] Fix: corrected typo, ^\\[minor\\].*, true",
    "Fix: [minor] corrected typo, ^\\[minor\\].*, false",
    "Update: improved performance, [minor], false"
  })
  public void testHasValueWithRegex(String commitMessage, String keyword, boolean expected) {
    VersionConfig versionConfig = VersionConfig.builder()
      .setRegexKeywords(true)
      .build();

    assertThat(GitUtil.hasValue(versionConfig, commitMessage, keyword)).isEqualTo(expected);
  }

  @Test
  public void testHasValueWithoutRegex() {
    VersionConfig versionConfig = VersionConfig.builder()
      .setRegexKeywords(false)
      .build();

    String commitMessage = "Fix: [minor] corrected typo";
    assertThat(GitUtil.hasValue(versionConfig, commitMessage, "[minor]")).isTrue();

    commitMessage = "Update: improved performance";
    assertThat(GitUtil.hasValue(versionConfig, commitMessage, "[minor]")).isFalse();
  }
}
