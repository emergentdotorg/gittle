package org.emergent.maven.gitver.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.emergent.maven.gitver.core.version.PatternStrategy;
import org.emergent.maven.gitver.core.version.ResolvedData;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class PropCodecTest {

    private static final TypeToken<Map<String, Object>> STR_OBJ_MAP_TT = new TypeToken<>() {
    };
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Test
    void testPatternStrategyToProperties() {
        Map<String, String> actual = getPatternStrategy().asMap();
        Map<String, String> expected = new LinkedHashMap<>();
        expected.putAll(getPatternStrategyProperties());
        assertThat(actual).isNotNull().isEqualTo(expected);
    }

    @Test
    void toPatternStrategy() {
        PatternStrategy expected = getPatternStrategy();
        Map<String, String> expectedProps = expected.asMap();

        PatternStrategy actual = PatternStrategy.from(expectedProps);
        Map<String, String> actualProps = actual.asMap();

        assertThat(GSON.toJson(actualProps, STR_OBJ_MAP_TT.getType()))
                .isNotNull()
                .isEqualTo(GSON.toJson(expectedProps, STR_OBJ_MAP_TT.getType()));

        assertThat(GSON.toJson(actual, PatternStrategy.class))
                .isNotNull()
                .isEqualTo(GSON.toJson(expected, PatternStrategy.class));
    }

    @Test
    void testResolvedDataToProperties() {
        Map<String, String> actual = getResolvedData().asMap();
        Properties expected = new Properties();
        expected.putAll(getResolvedDataProperties());
        assertThat(actual).isNotNull().isEqualTo(expected);
    }

    @Test
    void toResolvedData() {
        ResolvedData expected = getResolvedData();
        Map<String, String> expectedProps = expected.asMap();

        ResolvedData actual = PropCodec.fromProperties(expectedProps, ResolvedData.class);
        Map<String, String> actualProps = actual.asMap();

        assertThat(GSON.toJson(actualProps, STR_OBJ_MAP_TT.getType()))
                .isNotNull()
                .isEqualTo(GSON.toJson(expectedProps, STR_OBJ_MAP_TT.getType()));

        assertThat(GSON.toJson(actual, ResolvedData.class))
                .isNotNull()
                .isEqualTo(GSON.toJson(expected, ResolvedData.class));
    }

    @Test
    void testGitverConfigToProperties() {
        Properties actual = getGitverConfig().toProperties();
        Properties expected = new Properties();
        expected.putAll(getGitverConfigProperties());
        assertThat(actual).isNotNull().isEqualTo(expected);
    }

    @Test
    void toGitverConfig() {
        Map<String, String> props = getGitverConfigProperties();
        GitverConfig actual = GitverConfig.from(props);
        GitverConfig expected = getGitverConfig();
        assertThat(actual).isNotNull().isEqualTo(expected);
    }

    @Test
    void toXml_GitverConfig() throws Exception {
        Xpp3Dom expected = Xpp3DomBuilder.build(new StringReader("""
                <configuration>
                  <newVersion>0.1.2</newVersion>
                  <releaseBranches>release,stable</releaseBranches>
                  <tagNamePattern>v?([0-9]+\\.[0-9]+\\.[0-9]+)</tagNamePattern>
                  <versionPattern>%t(-%B)(-%c)(-%S)+%h(.%d)</versionPattern>
                </configuration>
                """));
        assertThat(PropCodec.toXml(getGitverConfig())).isNotNull().isEqualTo(expected);
    }

    // private static JsonElement toJsonElement(Object var) {
    //     if (var == null) {
    //         return JsonNull.INSTANCE;
    //     } else if (var instanceof Boolean v) {
    //         return new JsonPrimitive(v);
    //     } else if (var instanceof Number v) {
    //         return new JsonPrimitive(v);
    //     } else if (var instanceof String v) {
    //         return new JsonPrimitive(v);
    //     } else if (var instanceof Map<?, ?> v) {
    //         JsonObject object = new JsonObject();
    //         v.entrySet().stream()
    //           .filter(Allowed::isAllowedValue)
    //           .forEach(e -> object.add(String.valueOf(e.getKey()), toJsonElement(e.getValue())));
    //         return object;
    //     } else if (var instanceof Iterable<?> v) {
    //         JsonArray array = new JsonArray();
    //         StreamSupport.stream(v.spliterator(), false)
    //           .filter(Allowed::isAllowedValue)
    //           .forEach(v1 -> array.add(toJsonElement(v1)));
    //         return array;
    //     } else {
    //         throw new IllegalArgumentException("Unsupported type: " + var.getClass());
    //     }
    // }

    private Map<String, String> getPatternStrategyProperties() {
        return getPatternStrategy().asMap();
    }

    private Map<String, String> getResolvedDataProperties() {
        return getResolvedData().asMap();
    }

    private Map<String, String> getGitverConfigProperties() {
        return getGitverConfig().asMap();
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
                .hash("10fedcba")
                .commits(5)
                .dirty(true)
                .build();
    }

    private static GitverConfig getGitverConfig() {
        return GitverConfig.builder()
                .setReleaseBranches("release,stable")
                .setTagNamePattern("v?([0-9]+\\.[0-9]+\\.[0-9]+)")
                .setVersionPattern("%t(-%B)(-%c)(-%S)+%h(.%d)")
                .setNewVersion("0.1.2")
                .build();
    }
}