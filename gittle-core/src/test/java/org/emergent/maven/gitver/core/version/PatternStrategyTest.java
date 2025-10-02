package org.emergent.maven.gitver.core.version;

import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.Util;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class PatternStrategyTest {

    private static final HashMap<Object, Object> EMPTY = new HashMap<>();

    @Test
    public void testReleaseSansCommits() {
        PatternStrategy strategy = getPatternStrategy();
        assertThat(strategy.toVersionString()).isNotNull()
                .isEqualTo("1.2.3");
    }

    @Test
    public void testDevelSansCommits() {
        PatternStrategy strategy = getPatternStrategy();
        strategy = strategy.toBuilder()
                .releaseBranches("main")
                .branch("development")
                .build();
        assertThat(strategy.toVersionString()).isNotNull()
                .isEqualTo("1.2.3-development+c9f54782");
    }

    @Test
    public void testReleaseWithCommits() {
        PatternStrategy strategy = getPatternStrategy();
        strategy = strategy.toBuilder()
                .commits(1)
                .build();
        assertThat(strategy.toVersionString()).isNotNull()
                .isEqualTo("1.2.3-1-SNAPSHOT+c9f54782");
    }

    @Test
    public void testDevelopmentWithCommits() {
        PatternStrategy strategy = getPatternStrategy();
        strategy = strategy.toBuilder()
                .branch("development")
                .commits(1)
                .build();
        assertThat(strategy.toVersionString()).isNotNull()
                .isEqualTo("1.2.3-development-1-SNAPSHOT+c9f54782");
    }

    @Test
    public void testDirty() {
        PatternStrategy strategy = getPatternStrategy();
        strategy = strategy.toBuilder()
                .dirty(true)
                .build();
        assertThat(strategy.toVersionString()).isNotNull()
                .isEqualTo("1.2.3+c9f54782.dirty");
    }

    @Test
    public void testPatternSansHash() {
        PatternStrategy st = getPatternStrategy();
        PatternStrategy strategy = st.toBuilder()
                .versionPattern("%t(-%B)(-%c)(-%S)(.%d)")
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
        PatternStrategy reborn = Util.newPatternStrategy(props);
        assertThat(reborn).isEqualTo(strategy);
        PatternStrategy def = PatternStrategy.builder().build();
        Map<String, String> map = def.asMap();
        assertThat(map).isEqualTo(EMPTY);
    }

    @Test
    public void testPropertiesRoundTrip() {
        PatternStrategy strategy = getPatternStrategy();
        Map<String, String> props = strategy.asMap();
        PatternStrategy reborn = Util.newPatternStrategy(props);
        assertThat(reborn).isEqualTo(strategy);
        PatternStrategy def = PatternStrategy.builder().build();
        Map<String, String> map = def.asMap();
        assertThat(map).isEqualTo(EMPTY);
    }

    @Test
    public void testXmlOutput() {
        // PatternStrategy strategy = getStrategy();
        // String xml = PropCodec.toXml(strategy);
        // assertThat(xml).asString().isEqualTo("");
    }

    private static PatternStrategy getPatternStrategy() {
        ResolvedData resolved = getResolvedData();
        PatternStrategy.PatternStrategyBuilder<?, ?> build = PatternStrategy.builder()
                .newVersion(resolved.getNewVersion())
                .releaseBranches(resolved.getReleaseBranches())
                .tagNamePattern(resolved.getTagNamePattern())
                .versionPattern(resolved.getVersionPattern())
                .branch(resolved.getBranch())
                .hash(resolved.getHash())
                .tagged(resolved.getTagged())
                .commits(resolved.getCommits())
                .dirty(resolved.isDirty());
        return build.build();
    }

    private static ResolvedData getResolvedData() {
        GitverConfig config = getGitverConfig();
        return ResolvedData.builder()
                .newVersion(config.getNewVersion())
                .releaseBranches(config.getReleaseBranches())
                .tagNamePattern(config.getTagNamePattern())
                .versionPattern(config.getVersionPattern())
                .tagged("1.2.3")
                .branch("release")
                .hash("c9f54782")
                .commits(0)
                .dirty(false)
                .build();
    }

    private static GitverConfig getGitverConfig() {
        return GitverConfig.builder()
                .newVersion("0.1.2")
                .releaseBranches("release,stable")
                .tagNamePattern("v?([0-9]+\\.[0-9]+\\.[0-9]+)")
                .versionPattern("%t(-%B)(-%c)(-%S)(+%h)(.%d)")
                .build();
    }
}
