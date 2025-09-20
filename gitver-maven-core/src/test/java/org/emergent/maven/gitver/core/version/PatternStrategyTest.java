package org.emergent.maven.gitver.core.version;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.emergent.maven.gitver.core.GitverConfig;
import org.junit.jupiter.api.Test;

public class PatternStrategyTest {

    private static final HashMap<Object, Object> EMPTY = new HashMap<>();

    @Test
    public void testReleaseSansCommits() {
        PatternStrategy strategy = getPatternStrategy();
        assertThat(strategy.toVersionString()).isNotNull()
          .isEqualTo("1.2.3+c9f54782");
    }

    @Test
    public void testDevelSansCommits() {
        PatternStrategy strategy = getPatternStrategy().toBuilder()
          .setBranch("development")
          .build();
        assertThat(strategy.toVersionString()).isNotNull()
          .isEqualTo("1.2.3-development+c9f54782");
    }

    @Test
    public void testReleaseWithCommits() {
        PatternStrategy strategy = getPatternStrategy().toBuilder()
          .setCommits(1)
          .build();
        assertThat(strategy.toVersionString()).isNotNull()
          .isEqualTo("1.2.3-1-SNAPSHOT+c9f54782");
    }

    @Test
    public void testDevelopmentWithCommits() {
        PatternStrategy strategy = getPatternStrategy().toBuilder()
          .setBranch("development")
          .setCommits(1)
          .build();
        assertThat(strategy.toVersionString()).isNotNull()
          .isEqualTo("1.2.3-development-1-SNAPSHOT+c9f54782");
    }

    @Test
    public void testDirty() {
        PatternStrategy strategy = getPatternStrategy().toBuilder()
          .setDirty(true)
          .build();
        assertThat(strategy.toVersionString()).isNotNull()
          .isEqualTo("1.2.3+c9f54782.dirty");
    }

    @Test
    public void testPatternSansHash() {
        PatternStrategy st = getPatternStrategy();
        PatternStrategy strategy = st.toBuilder()
                  .setConfig(st.config().toBuilder()
                    .setVersionPattern("%t(-%B)(-%c)(-%S)(.%d)")
                    .build())
                  .build();
        assertThat(strategy.toVersionString()).isNotNull()
          .isEqualTo("1.2.3");
    }

    @Test
    public void testPropertiesRoundTrip() {
        PatternStrategy strategy = getPatternStrategy();
        Properties props = strategy.toProperties();
        PatternStrategy reborn = PatternStrategy.from(props);
        assertThat(reborn).isEqualTo(strategy);

        PatternStrategy def = PatternStrategy.builder().build();
        Properties map = def.toProperties();
        assertThat(map).isEqualTo(EMPTY);
    }

    @Test
    public void testXmlOutput() {
        // PatternStrategy strategy = getStrategy();
        // String xml = PropCodec.toXml(strategy);
        // assertThat(xml).asString().isEqualTo("");
    }

    private static PatternStrategy getPatternStrategy() {
        return PatternStrategy.builder()
          .setConfig(getGitverConfig())
          .setTagged("1.2.3")
          .setBranch("release")
          .setHash("c9f54782")
          .setCommits(0)
          .setDirty(false)
          .build();
    }

    private static GitverConfig getGitverConfig() {
        return GitverConfig.builder()
          .setReleaseBranches("release,stable")
          .setTagPattern("v?([0-9]+\\.[0-9]+\\.[0-9]+)")
          .setVersionPattern("%t(-%B)(-%c)(-%S)+%h(.%d)")
          .setVersionOverride("0.1.2")
          .build();
    }}
