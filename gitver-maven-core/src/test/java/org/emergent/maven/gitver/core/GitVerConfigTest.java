package org.emergent.maven.gitver.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class GitVerConfigTest {

  @Test
  public void getDefaults() {
    GitVerConfig config = GitVerConfig.builder().build();
    assertThat(config)
      .extracting(
        "disabled",
        "initialMajor", "initialMinor", "initialPatch",
        "majorKey", "minorKey", "patchKey",
        "useRegex", "versionPattern", "versionOverride")
      .containsExactly(
        false,
        0, 0, 0,
        GitVerConfig.KEY_MAJOR, GitVerConfig.KEY_MINOR, GitVerConfig.KEY_PATCH,
        false, GitVerConfig.DEFAULT_VERSION_PATTERN, null);
  }

  @Test
  public void setInitialValues() {
    GitVerConfig config = GitVerConfig.builder()
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
    GitVerConfig versionConfig = GitVerConfig.builder()
      .setMajorKey("[TEST1]")
      .setMinorKey("[TEST2]")
      .setPatchKey("[TEST3]")
      .setUseRegex(true)
      .build();
    assertThat(versionConfig)
      .extracting("majorKey", "minorKey", "patchKey", "useRegex")
      .containsExactly("[TEST1]", "[TEST2]", "[TEST3]", true);
  }

  @Test
  public void setMiscellaneous() {
    GitVerConfig config = GitVerConfig.builder()
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
    Throwable ex = catchThrowable(() -> GitVerConfig.builder().setInitialMajor(-1).build());
    assertThat(ex)
      .isNotNull()
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("InitialVersion.major must be a non-negative integer");
  }

  @Test
  public void setInitialMinor_negative() {
    Throwable ex = catchThrowable(() -> GitVerConfig.builder().setInitialMinor(-1).build());
    assertThat(ex)
      .isNotNull()
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("InitialVersion.minor must be a non-negative integer");
  }

  @Test
  public void setInitialPatch_negative() {
    Throwable ex = catchThrowable(() -> GitVerConfig.builder().setInitialPatch(-1).build());
    assertThat(ex)
      .isNotNull()
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("InitialVersion.patch must be a non-negative integer");
  }

}
