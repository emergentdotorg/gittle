package org.emergent.maven.gitver.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.emergent.maven.gitver.core.Constants.VERSION_PATTERN_DEF;

import org.junit.jupiter.api.Test;

public class GitverConfigTest {

  @Test
  public void getDefaults() {
    GitverConfig config = GitverConfig.builder().build();
    assertThat(config)
      .extracting("versionPattern", "versionOverride")
      .containsExactly(VERSION_PATTERN_DEF, "");
  }

  @Test
  public void setMiscellaneous() {
    GitverConfig config = GitverConfig.builder()
      .setVersionPattern("%t(-%c)")
      .setVersionOverride("1.2.3-SNAPSHOT")
      .build();
    assertThat(config)
      .extracting("versionPattern", "versionOverride")
      .containsExactly("%t(-%c)", "1.2.3-SNAPSHOT");
  }
}
