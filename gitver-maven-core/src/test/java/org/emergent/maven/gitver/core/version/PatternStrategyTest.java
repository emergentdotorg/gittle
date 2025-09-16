package org.emergent.maven.gitver.core.version;

import static org.assertj.core.api.Assertions.assertThat;

import org.emergent.maven.gitver.core.GitverConfig;
import org.junit.jupiter.api.Test;

public class PatternStrategyTest {

    @Test
    public void testReleaseSansCommits() {
        PatternStrategy st = getStrategy();
        PatternStrategy strategy = st.toBuilder()
          .build();
        String expected = "1.2.3+c9f54782";
        assertThat(strategy.toVersionString()).asString().isEqualTo(expected);
    }

    @Test
    public void testDevelSansCommits() {
        PatternStrategy st = getStrategy();
        PatternStrategy strategy = st.toBuilder()
          .setBranch("development")
          .build();
        String expected = "1.2.3-development+c9f54782";
        assertThat(strategy.toVersionString()).asString().isEqualTo(expected);
    }

    @Test
    public void testReleaseWithCommits() {
        PatternStrategy st = getStrategy();
        PatternStrategy strategy = st.toBuilder()
          .setCommits(1)
          .build();
        String expected = "1.2.3-1-SNAPSHOT+c9f54782";
        assertThat(strategy.toVersionString()).asString().isEqualTo(expected);
    }

    @Test
    public void testDevelopmentWithCommits() {
        PatternStrategy st = getStrategy();
        PatternStrategy strategy = st.toBuilder()
          .setBranch("development")
          .setCommits(1)
          .build();
        String expected = "1.2.3-development-1-SNAPSHOT+c9f54782";
        assertThat(strategy.toVersionString()).asString().isEqualTo(expected);
    }

    @Test
    public void testDirty() {
        PatternStrategy st = getStrategy();
        PatternStrategy strategy = st.toBuilder()
          .setDirty(true)
          .build();
        String expected = "1.2.3+c9f54782.dirty";
        assertThat(strategy.toVersionString()).asString().isEqualTo(expected);
    }

    @Test
    public void testPatternSansHash() {
        PatternStrategy st = getStrategy();
        PatternStrategy strategy = st.toBuilder()
          .setConfig(GitverConfig.builder()
            .setVersionPattern("%t(-%B)(-%c)(-%S)(.%d)")
            .build())
          .build();
        String expected = "1.2.3";
        assertThat(strategy.toVersionString()).asString().isEqualTo(expected);
    }

    private static PatternStrategy getStrategy() {
        return PatternStrategy.builder()
          .setTag("1.2.3")
          .setBranch("main")
          .setHash("c9f54782")
          .setCommits(0)
          .setDirty(false)
          .setConfig(GitverConfig.builder()
            .setVersionPattern("%t(-%B)(-%c)(-%S)+%h(.%d)")
            .build())
          .build();
    }
}
