package org.emergent.maven.gitver.core;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.emergent.maven.gitver.core.version.ResolvedData;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.emergent.maven.gitver.core.GsonUtil.STR_STR_MAP_TT;

class PropCodecTest {

    private static final TypeToken<Map<String, Object>> STR_OBJ_MAP_TT = new TypeToken<>() {
    };
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

//    @Test
//    void testPatternStrategyToProperties() {
//        Map<String, String> actual = getPatternStrategy().asMap();
//        Map<String, String> expected = new LinkedHashMap<>();
//        expected.putAll(getPatternStrategyProperties());
//        assertThat(actual).isNotNull().isEqualTo(expected);
//    }
//
//    @Test
//    void toPatternStrategy() {
//        PatternStrategy expected = getPatternStrategy();
//        Map<String, String> expectedProps = expected.asMap();
//
//        PatternStrategy actual = PatternStrategy.from(expectedProps);
//        Map<String, String> actualProps = actual.asMap();
//
//        assertThat(GSON.toJson(actualProps, STR_OBJ_MAP_TT.getType()))
//                .isNotNull()
//                .isEqualTo(GSON.toJson(expectedProps, STR_OBJ_MAP_TT.getType()));
//
//        assertThat(GSON.toJson(actual, PatternStrategy.class))
//                .isNotNull()
//                .isEqualTo(GSON.toJson(expected, PatternStrategy.class));
//    }

    @Test
    void testResolvedDataToProperties() {
        Map<String, String> expected = getResolvedDataProperties();
        Map<String, String> actual = PropCodec.toProperties(getResolvedData());
        assertThat(actual).isNotNull()
                        .as(() -> GSON.toJson(Map.of(
                                "actual", actual,
                                "expected", expected
                        ), STR_OBJ_MAP_TT.getType()))
                .isEqualTo(expected);
    }

    @Test
    void toResolvedData() {
        ResolvedData expected = getResolvedData();
        Map<String, String> expectedProps = getResolvedDataProperties();

        ResolvedData actual = PropCodec.fromProperties(expectedProps, ResolvedData.class);
        Map<String, String> actualProps = PropCodec.toProperties(actual);

//        assertThat(expectedProps.getClass())
//                .isNotNull()
//                .isEqualTo(LinkedHashMap.class);

        assertThat(expectedProps.keySet().stream().toList())
                .isNotNull()
                .isEqualTo(expectedProps.keySet().stream().sorted().toList());

        assertThat(join(new TreeMap<>(actualProps)))
                .isNotNull()
                .isEqualTo(join(new TreeMap<>(expectedProps)));

        assertThat(join(actualProps))
                .isNotNull()
                .isEqualTo(join(expectedProps));

        assertThat(GSON.toJson(actual, ResolvedData.class))
                .isNotNull()
                .isEqualTo(GSON.toJson(expected, ResolvedData.class));
    }

    @Test
    void testGitverConfigToProperties() {
        Map<String, String> expected = getGitverConfigProperties();
        Map<String, String> actual = PropCodec.toProperties(getGitverConfig());
        assertThat(actual).isNotNull()
                .as(() -> GSON.toJson(Map.of(
                        "actual", actual,
                        "expected", expected
                ), STR_OBJ_MAP_TT.getType()))
                .isEqualTo(expected);
    }

    @Test
    void toGitverConfig() {
        GitverConfig expected = getGitverConfig();
        Map<String, String> expectedProps = getGitverConfigProperties();

        GitverConfig actual = PropCodec.fromProperties(expectedProps, GitverConfig.class);
        Map<String, String> actualProps = PropCodec.toProperties(actual);

        assertThat(GSON.toJson(actualProps))
                .isNotNull()
                .isEqualTo(GSON.toJson(expectedProps));

        assertThat(GSON.toJson(actual, GitverConfig.class))
                .isNotNull()
                .isEqualTo(GSON.toJson(expected, GitverConfig.class));
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

//    private Map<String, String> getPatternStrategyProperties() {
//        return PropCodec.toProperties(getPatternStrategy());
//    }

    private Map<String, String> getResolvedDataProperties() {
        if (false) {
            return Map.of(
                    "gittle.resolved.branch", "release",
                    "gittle.resolved.commits", "5",
                    "gittle.resolved.dirty", "true",
                    "gittle.resolved.hash", "10fedcba",
                    "gittle.resolved.tagged", "1.2.3"
            );
        } else {
            return flattenMap(Map.of(
                    "gittle.resolved.branch", "release",
                    "gittle.resolved.commits", 5,
                    "gittle.resolved.dirty", true,
                    "gittle.resolved.hash", "10fedcba",
                    "gittle.resolved.tagged", "1.2.3"
            ));
        }
    }

    private Map<String, String> getGitverConfigProperties() {
        return flattenMap(Map.of(
                "gittle.newVersion", "0.1.2",
                "gittle.releaseBranches", "release,stable"
        ));
    }

//    private static PatternStrategy getPatternStrategy() {
//        return PatternStrategy.builder()
//                .setConfig(getGitverConfig())
//                .setResolved(getResolvedData())
//                .build();
//    }

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

    private static Map<String, String> flattenMap(Map<String, ?> src) {
        return PropCodec.flattenMap(new TreeMap<>(src));
//        return src.entrySet().stream()
//                .collect(CollectorsEx.toTreeMap(Map.Entry::getKey, e -> e.getValue().toString()));
//        return new TreeSet<>(src.keySet()).stream()
//                .map(k -> Map.entry(k, String.valueOf(src.get(k))))
//                .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
//        if (src.values().stream().noneMatch(v -> (v instanceof Map) || (v instanceof List))) {
//            return src.entrySet().stream()
//                    .collect(CollectorsEx.toLinkedHashMap(
//                            e -> String.valueOf(e.getKey()),
//                            e -> String.valueOf(e.getValue())
//                    ));
//        }
    }

    private static String join(Map<String, String> map) {
        return Util.join(map);
    }
}