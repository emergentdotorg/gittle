package org.emergent.maven.gitver.core;

import org.emergent.maven.gitver.core.version.BasicVersion;
import org.emergent.maven.gitver.core.version.KeywordsConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GitverConfigTest {

  @Test
  public void getDefaults() {
    GitverConfig config = GitverConfig.builder().build();
    assertThat(config)
      .extracting(
        "disabled",
        "initial", "keywords",
        "versionPattern", "versionOverride")
      .containsExactly(
        false,
        BasicVersion.zero(), KeywordsConfig.builder().build(),
        Constants.DEFAULT_VERSION_PATTERN, "");
  }

  @Test
  public void setInitialValues() {
    GitverConfig config = GitverConfig.builder()
      .setInitial(BasicVersion.builder()
        .setMajor(1)
        .setMinor(2)
        .setPatch(3)
        .build())
      .build();
    assertThat(config)
      .extracting("initial").asString().isEqualTo("1.2.3");
  }

  @Test
  public void setMiscellaneous() {
    GitverConfig config = GitverConfig.builder()
      .setDisabled(true)
      .setVersionPattern("%M.%m.%p(-%c)")
      .setVersionOverride("1.2.3-SNAPSHOT")
      .build();
    assertThat(config)
      .extracting("disabled", "versionPattern", "versionOverride")
      .containsExactly(true, "%M.%m.%p(-%c)", "1.2.3-SNAPSHOT");
  }
}
