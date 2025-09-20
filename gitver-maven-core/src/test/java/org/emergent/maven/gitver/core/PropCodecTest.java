package org.emergent.maven.gitver.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.emergent.maven.gitver.core.version.PatternStrategy;
import org.junit.jupiter.api.Test;

class PropCodecTest {

    private static final TypeToken<Map<String, Object>> STR_OBJ_MAP_TT = new TypeToken<>() {};
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final PropCodec codec = PropCodec.getInstance();

    @Test
    void testPatternStrategyToProperties() {
        Properties actual = getPatternStrategy().toProperties();
        Properties expected = new Properties();
        expected.putAll(getPatternStrategyProperties());
        assertThat(actual).isNotNull().isEqualTo(expected);
    }

    @Test
    void testGitverConfigToProperties() {
        Properties actual = getGitverConfig().toProperties();
        Properties expected = new Properties();
        expected.putAll(getGitverConfigProperties());
        assertThat(actual).isNotNull().isEqualTo(expected);
    }

    @Test
    void toPatternStrategy() {
        PatternStrategy expected = getPatternStrategy();
        Properties expectedProps = expected.toProperties();

        PatternStrategy actual = codec.fromProperties(expectedProps, PatternStrategy.class);
        Properties actualProps = actual.toProperties();

        // Optional.ofNullable(actualMap.get("config"))
        //   .filter(Map.class::isInstance).map(Map.class::cast)
        //   .ifPresent(c -> actualMap.put("config", new TreeMap<String, Object>(c)));
        //
        // Optional.ofNullable(expectedMap.get("config"))
        //   .filter(Map.class::isInstance).map(Map.class::cast)
        //   .ifPresent(c -> expectedMap.put("config", new TreeMap<String, Object>(c)));

        // assertThat(GSON.toJson(new TreeMap<>(fixedProps), PropCodec.STR_STR_MAP_TT.getType()))
        //   .isNotNull()
        //   .isEqualTo(GSON.toJson(new TreeMap<>(expectedProps), PropCodec.OBJ_OBJ_MAP_TT.getType()));

        /*
        assertThat(GSON.toJson(new TreeMap<>(fixedMap), PropCodec.STR_STR_MAP_TT.getType()))
          .isNotNull()
          .isEqualTo(GSON.toJson(new TreeMap<>(expectedMap), PropCodec.OBJ_OBJ_MAP_TT.getType()));
        */

        assertThat(GSON.toJson(actualProps, STR_OBJ_MAP_TT.getType()))
          .isNotNull()
          .isEqualTo(GSON.toJson(expectedProps, STR_OBJ_MAP_TT.getType()));

        // assertThat(GSON.toJson(actualProps, PropCodec.OBJ_OBJ_MAP_TT.getType()))
        //   .isNotNull()
        //   .isEqualTo(GSON.toJson(expectedProps, PropCodec.OBJ_OBJ_MAP_TT.getType()));

        assertThat(GSON.toJson(actual, PatternStrategy.class))
          .isNotNull()
          .isEqualTo(GSON.toJson(expected, PatternStrategy.class));
    }

    @Test
    void toGitverConfig() {
        Properties props = getGitverConfigProperties();
        GitverConfig actual = GitverConfig.from(props);
        GitverConfig expected = getGitverConfig();
        assertThat(actual).isNotNull().isEqualTo(expected);
    }

    @Test
    void toToDottedKeys() {
        Map<String, String> actual = codec.toDotted(Map.of(
                "astring",
                "stringx",
                "aboolean",
                true,
                "anumber",
                5,
                "alist",
                List.of("stringy", true, 7),
                "amap",
                Map.of("astring", "stringz", "aboolean", false, "anumber", 9)));
        assertThat(actual)
                .isNotNull()
                // .isInstanceOf(JsonObject.class)
                .isEqualTo(codec.toDotted(Map.of(
                        "astring",
                        "stringx",
                        "aboolean",
                        true,
                        "anumber",
                        5,
                        "alist.1",
                        "stringy",
                        "alist.2",
                        true,
                        "alist.3",
                        7,
                        "amap.astring",
                        "stringz",
                        "amap.aboolean",
                        false,
                        "amap.anumber",
                        9)));
    }

    @Test
    void fromToDottedKeys() {
        Map<String, Object> actual = codec.toUndotted(Map.of(
                "astring", "stringx",
                "aboolean", true,
                "anumber", 5,
                "alist.1", "stringy",
                "alist.2", true,
                "alist.3", 7,
                "amap.astring", "stringz",
                "amap.aboolean", false,
                "amap.anumber", 9
        ));
        Map<String, Object> expected = Map.of(
                "astring", "stringx",
                "aboolean", true,
                "anumber", 5,
                "alist", List.of(
                        "stringy",
                        true,
                        7
                ),
                "amap", Map.of(
                        "astring", "stringz",
                        "aboolean", false,
                        "anumber", 9
                )
        );
        assertThat(actual)
                .isNotNull()
                .as(() -> GSON.toJson(Map.of("actual", actual, "expected", expected)))
                // .isInstanceOf(JsonObject.class)
                .isEqualTo(expected);
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

    private Properties getPatternStrategyProperties() {
        return getPatternStrategy().toProperties();
    }

    private Properties getGitverConfigProperties() {
        return getGitverConfig().toProperties();
    }

    private static PatternStrategy getPatternStrategy() {
        return PatternStrategy.builder()
          .setConfig(getGitverConfig())
          .setTagged("1.2.3")
          .setBranch("release")
          .setHash("10fedcba")
          .setCommits(5)
          .setDirty(true)
          .build();
    }

    private static GitverConfig getGitverConfig() {
        return GitverConfig.builder()
          .setReleaseBranches("release,stable")
          .setTagPattern("v?([0-9]+\\.[0-9]+\\.[0-9]+)")
          .setVersionPattern("%t(-%B)(-%c)(-%S)+%h(.%d)")
          .setVersionOverride("0.1.2")
          .build();
    }
}