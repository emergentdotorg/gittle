package org.emergent.maven.gitver.core;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.emergent.maven.gitver.core.Constants.VERSION_PATTERN_DEF;

public class GitverConfigTest {

    private static final HashMap<Object, Object> EMPTY = new HashMap<>();

    @Test
    public void getDefaults() {
        GitverConfig config = GitverConfig.builder().build();
        assertThat(config)
          .extracting("versionPattern", "newVersion")
          .containsExactly(VERSION_PATTERN_DEF, "");
    }

    @Test
    public void setMiscellaneous() {
        GitverConfig config = getGitverConfig();
        assertThat(config)
          .extracting("versionPattern", "newVersion")
          .containsExactly("%t(-%c)", "0.1.2");
    }

    @Test
    public void testPropertiesRoundTrip() {
        GitverConfig config = getGitverConfig();
        Map<String, String> props = config.asMap();
        GitverConfig reborn = Util.newGitverConfig(props);
        assertThat(reborn).isEqualTo(config);

        GitverConfig def = GitverConfig.builder().build();
        Map<String, String> map = def.asMap();
        assertThat(map).isEqualTo(EMPTY);
    }

    private static GitverConfig getGitverConfig() {
        return GitverConfig.builder()
          .releaseBranches("release,stable")
          .tagNamePattern("v?([0-9]+\\.[0-9]+\\.[0-9]+)")
          .versionPattern("%t(-%c)")
          .newVersion("0.1.2")
          .build();
    }
}
