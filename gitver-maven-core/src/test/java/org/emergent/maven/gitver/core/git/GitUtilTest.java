package org.emergent.maven.gitver.core.git;

import org.emergent.maven.gitver.core.GitVerConfig;
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
    GitVerConfig versionConfig = GitVerConfig.builder()
      .setUseRegex(true)
      .build();

    assertThat(GitUtil.hasValue(versionConfig, commitMessage, keyword)).isEqualTo(expected);
  }

  @Test
  public void testHasValueWithoutRegex() {
    GitVerConfig versionConfig = GitVerConfig.builder()
      .setUseRegex(false)
      .build();

    String commitMessage = "Fix: [minor] corrected typo";
    assertThat(GitUtil.hasValue(versionConfig, commitMessage, "[minor]")).isTrue();

    commitMessage = "Update: improved performance";
    assertThat(GitUtil.hasValue(versionConfig, commitMessage, "[minor]")).isFalse();
  }
}
