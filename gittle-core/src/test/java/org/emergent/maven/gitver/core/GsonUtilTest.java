package org.emergent.maven.gitver.core;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GsonUtilTest {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

//    private final GsonUtil gsonUtil = GsonUtil.getInstance(true);

    @Test
    void toFlattenKeys() {
        Map<String, String> actual = PropCodec.toProperties(ImmutableMap.of(
                "astring",
                "stringx",
                "aboolean",
                true,
                "anumber",
                5,
                "alist",
                List.of("stringy", true, 7),
                "amap",
                Map.of("astring", "stringz", "aboolean", false, "anumber", 9)
        )).getProperties();
        assertThat(actual)
                .isNotNull()
                // .isInstanceOf(JsonObject.class)
                .isEqualTo(PropCodec.toProperties(ImmutableMap.of(
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
                        9)
                ).getProperties());
    }

    @Test
    void fromFlattenKeys() {
        Map<String, Object> actual = PropCodec.toMap(PropCodec.toJsonTree(ImmutableMap.of(
                "astring", "stringx",
                "aboolean", true,
                "anumber", 5L,
                "alist.1", "stringy",
                "alist.2", true,
                "alist.3", 7L,
                "amap.astring", "stringz",
                "amap.aboolean", false,
                "amap.anumber", 9L
        )));
        Map<String, Object> expected = ImmutableMap.of(
                "astring", "stringx",
                "aboolean", true,
                "anumber", 5L,
                "alist", List.of(
                        "stringy",
                        true,
                        7L
                ),
                "amap", ImmutableMap.of(
                        "astring", "stringz",
                        "aboolean", false,
                        "anumber", 9L
                )
        );
        assertThat(actual)
                .as(() -> GSON.toJson(Map.of("actual", actual, "expected", expected)))
                .isNotNull()
                .isEqualTo(expected);
    }
}