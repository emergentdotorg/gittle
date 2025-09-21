package org.emergent.maven.gitver.core.version;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
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
        PatternStrategy strategy = getPatternStrategy();
        strategy = strategy.toBuilder()
                .setResolved(strategy.getResolved().toBuilder().branch("development"))
                .build();
        assertThat(strategy.toVersionString()).isNotNull()
          .isEqualTo("1.2.3-development+c9f54782");
    }

    @Test
    public void testReleaseWithCommits() {
        PatternStrategy strategy = getPatternStrategy();
        strategy = strategy.toBuilder()
                .setResolved(strategy.getResolved().toBuilder()
                        .commits(1))
                .build();
        assertThat(strategy.toVersionString()).isNotNull()
          .isEqualTo("1.2.3-1-SNAPSHOT+c9f54782");
    }

    @Test
    public void testDevelopmentWithCommits() {
        PatternStrategy strategy = getPatternStrategy();
        strategy = strategy.toBuilder()
                .setResolved(strategy.getResolved().toBuilder()
                        .branch("development")
                        .commits(1))
                .build();
        assertThat(strategy.toVersionString()).isNotNull()
          .isEqualTo("1.2.3-development-1-SNAPSHOT+c9f54782");
    }

    @Test
    public void testDirty() {
        PatternStrategy strategy = getPatternStrategy();
        strategy = strategy.toBuilder()
                .setResolved(strategy.getResolved().toBuilder()
                        .dirty(true))
                .build();
        assertThat(strategy.toVersionString()).isNotNull()
          .isEqualTo("1.2.3+c9f54782.dirty");
    }

    @Test
    public void testPatternSansHash() {
        PatternStrategy st = getPatternStrategy();
        PatternStrategy strategy = st.toBuilder()
                  .setConfig(st.getConfig().toBuilder()
                    .setVersionPattern("%t(-%B)(-%c)(-%S)(.%d)")
                    .build())
                  .build();
        assertThat(strategy.toVersionString()).isNotNull()
          .isEqualTo("1.2.3");
    }

    @Test
    public void testPropertiesNames() {
        PatternStrategy strategy = getPatternStrategy();
        Map<String, String> props = strategy.asMap();
        String collect = props.entrySet().stream()
          .map(e -> e.getKey() + "=" + e.getValue())
          .collect(Collectors.joining("\n\t", "\n\t", "\n"));
        System.out.printf("props:%s%n", collect);
        PatternStrategy reborn = PatternStrategy.from(props);
        assertThat(reborn).isEqualTo(strategy);
        PatternStrategy def = PatternStrategy.create();
        Properties map = def.toProperties();
//        assertThat(map).isEqualTo(EMPTY);
    }

    @Test
    public void testPropertiesRoundTrip() {
        PatternStrategy strategy = getPatternStrategy();
        Map<String, String> props = strategy.asMap();

        PatternStrategy reborn = PatternStrategy.from(props);
        assertThat(reborn).isEqualTo(strategy);
        PatternStrategy def = PatternStrategy.create();
        Properties map = def.toProperties();
//        assertThat(map).isEqualTo(EMPTY);
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
          .setResolved(getResolvedData())
          .build();
    }
    private static ResolvedData getResolvedData() {
        return ResolvedData.builder()
                .tagged("1.2.3")
                .branch("release")
                .hash("c9f54782")
                .commits(0)
                .dirty(false)
                .build();
    }

    private static GitverConfig getGitverConfig() {
        return GitverConfig.builder()
          .setReleaseBranches("release,stable")
          .setTagNamePattern("v?([0-9]+\\.[0-9]+\\.[0-9]+)")
          .setVersionPattern("%t(-%B)(-%c)(-%S)+%h(.%d)")
          .setNewVersion("0.1.2")
          .build();
    }}
