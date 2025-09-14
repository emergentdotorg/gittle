package org.emergent.maven.gitver.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.emergent.maven.gitver.core.Constants.VERSION_INITAL_DEF;
import static org.emergent.maven.gitver.core.Constants.VERSION_PATTERN_DEF;
import static org.emergent.maven.gitver.core.version.KeywordsConfig.DEFAULT_KEYWORDS;

import org.junit.jupiter.api.Test;

public class GitverConfigTest {

  @Test
  public void getDefaults() {
    GitverConfig config = GitverConfig.builder().build();
    assertThat(config)
      .extracting("versionInitial", "keywords", "versionPattern", "versionOverride")
      .containsExactly(VERSION_INITAL_DEF, DEFAULT_KEYWORDS, VERSION_PATTERN_DEF, "");
  }

  @Test
  public void setInitialValues() {
    GitverConfig config = GitverConfig.builder()
      .setVersionInitial("1.2.3")
      .build();
    assertThat(config)
      .extracting("versionInitial").asString().isEqualTo("1.2.3");
  }

  @Test
  public void setMiscellaneous() {
    GitverConfig config = GitverConfig.builder()
      .setVersionPattern("%M.%m.%p(-%c)")
      .setVersionOverride("1.2.3-SNAPSHOT")
      .build();
    assertThat(config)
      .extracting("versionPattern", "versionOverride")
      .containsExactly("%M.%m.%p(-%c)", "1.2.3-SNAPSHOT");
  }
}
