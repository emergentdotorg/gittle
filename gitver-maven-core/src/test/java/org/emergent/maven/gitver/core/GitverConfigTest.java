package org.emergent.maven.gitver.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.emergent.maven.gitver.core.Constants.VERSION_PATTERN_DEF;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

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
        GitverConfig reborn = GitverConfig.from(props);
        assertThat(reborn).isEqualTo(config);

        GitverConfig def = GitverConfig.builder().build();
        Map<String, String> map = def.asMap();
        assertThat(map).isEqualTo(EMPTY);
    }

    private static GitverConfig getGitverConfig() {
        return GitverConfig.builder()
          .setReleaseBranches("release,stable")
          .setTagNamePattern("v?([0-9]+\\.[0-9]+\\.[0-9]+)")
          .setVersionPattern("%t(-%c)")
          .setNewVersion("0.1.2")
          .build();
    }
}
