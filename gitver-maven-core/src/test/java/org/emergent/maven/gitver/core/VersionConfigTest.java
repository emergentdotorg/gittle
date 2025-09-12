package org.emergent.maven.gitver.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class VersionConfigTest {

  @Test
  public void getDefaults() {
    VersionConfig config = VersionConfig.builder().build();
    assertThat(config)
      .extracting(
        "disabled",
        "initialMajor", "initialMinor", "initialPatch",
        "majorKeywords", "minorKeywords", "patchKeywords",
        "regexKeywords", "versionPattern", "versionOverride")
      .containsExactly(
        false,
        0, 0, 0,
        Constants.DEFAULT_MAJOR_KEYWORD, Constants.DEFAULT_MINOR_KEYWORD, Constants.DEFAULT_PATCH_KEYWORD,
        false, Constants.DEFAULT_VERSION_PATTERN, "");
  }

  @Test
  public void setInitialValues() {
    VersionConfig config = VersionConfig.builder()
      .setInitialMajor(1)
      .setInitialMinor(2)
      .setInitialPatch(3)
      .build();
    assertThat(config)
      .extracting("initialMajor", "initialMinor", "initialPatch")
      .containsExactly(1, 2, 3);
  }

  @Test
  public void setKeywordValues() {
    VersionConfig versionConfig = VersionConfig.builder()
      .setMajorKeywords("[TEST1]")
      .setMinorKeywords("[TEST2]")
      .setPatchKeywords("[TEST3]")
      .setRegexKeywords(true)
      .build();
    assertThat(versionConfig)
      .extracting("majorKeywords", "minorKeywords", "patchKeywords", "regexKeywords")
      .containsExactly("[TEST1]", "[TEST2]", "[TEST3]", true);
  }

  @Test
  public void setMiscellaneous() {
    VersionConfig config = VersionConfig.builder()
      .setDisabled(true)
      .setVersionPattern("%M.%m.%p(-%c)")
      .setVersionOverride("1.2.3-SNAPSHOT")
      .build();
    assertThat(config)
      .extracting("disabled", "versionPattern", "versionOverride")
      .containsExactly(true, "%M.%m.%p(-%c)", "1.2.3-SNAPSHOT");
  }

  @Test
  public void setInitialMajor_negative() {
    Throwable ex = catchThrowable(() -> VersionConfig.builder().setInitialMajor(-1).build());
    assertThat(ex)
      .isNotNull()
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("gv.initial.major must be a non-negative integer");
  }

  @Test
  public void setInitialMinor_negative() {
    Throwable ex = catchThrowable(() -> VersionConfig.builder().setInitialMinor(-1).build());
    assertThat(ex)
      .isNotNull()
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("gv.initial.minor must be a non-negative integer");
  }

  @Test
  public void setInitialPatch_negative() {
    Throwable ex = catchThrowable(() -> VersionConfig.builder().setInitialPatch(-1).build());
    assertThat(ex)
      .isNotNull()
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("gv.initial.patch must be a non-negative integer");
  }

}
