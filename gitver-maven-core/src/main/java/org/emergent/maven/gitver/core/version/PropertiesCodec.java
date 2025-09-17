package org.emergent.maven.gitver.core.version;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.Util;

public class PropertiesCodec {

    public static final TypeToken<Map<String, Object>> MAP_TYPE_TOKEN = new TypeToken<>() {};
    public static final TypeToken<PatternStrategy> PATTERN_STRATEGY_TT = new TypeToken<>() {};
    public static final TypeToken<GitverConfig> GITVER_CONFIG_TYPE_TOKEN = new TypeToken<>() {};
    // private static final TypeToken<PatternStrategy> CONFIG_BASE_TYPE_TOKEN = new TypeToken<>() {};

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static PatternStrategy toPatternStrategy(Map<String, ?> map) {
        return fromMap(map, PATTERN_STRATEGY_TT.getType());
    }

    public static GitverConfig toGitverConfig(Map<String, ?> map) {
        return fromMap(map, GITVER_CONFIG_TYPE_TOKEN.getType());
    }

    public static GitverConfig toGitverConfig(Properties map) {
        return toGitverConfig(Util.flatten(map));
    }

    public static Map<String, Object> toMap(PatternStrategy src) {
        return toMap(src, PATTERN_STRATEGY_TT.getType());
    }

    public static Map<String, Object> toMap(GitverConfig src) {
        return toMap(src, GITVER_CONFIG_TYPE_TOKEN.getType());
    }

    public static Properties toProperties(PatternStrategy src) {
        Properties props = new Properties();
        props.putAll(toMap(src));
        return props;
    }

    public static Properties toProperties(GitverConfig src) {
        Properties props = new Properties();
        props.putAll(toMap(src));
        return props;
    }

    public static <T> T fromMap(Map<String, ?> oldMap, Type typeOfSrc) {
        JsonElement json = toTree(oldMap);
        return gson.fromJson(json, typeOfSrc);
    }

    public static Map<String, Object> toMap(Object src, Type typeOfSrc) {
        JsonElement json = gson.toJsonTree(src, typeOfSrc);
        Map<String, Object> oldMap = gson.fromJson(json, MAP_TYPE_TOKEN.getType());
        Map<String, Object> outMap = prefixKeys(oldMap);
        log(outMap);
        return outMap;
    }

    public static JsonElement toTree(Map<String, ?> oldMap) {
        Map<String, Object> map = shortenKeys(new LinkedHashMap<>(oldMap));
        JsonElement json = gson.toJsonTree(map, MAP_TYPE_TOKEN.getType());
        return json;
        // gson.fromJson(json, CONFIG_BASE_TYPE_TOKEN.getType());
    }

    private static void log(Map<String, Object> outMap) {
        System.out.printf(
                "PatternStrategy properties: %s%n",
                outMap.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(Collectors.joining("\n", "\n", "\n")));
    }

    public static <V> Map<String, V> shortenKeys(Map<String, V> oldMap) {
        Map<String, V> outMap = new LinkedHashMap<>();
        oldMap.forEach((k, v) -> {
            outMap.put(StringUtils.stripStart(k, "gv."), v);
        });
        // Properties p = new Properties();
        // p.putAll(outMap);
        return outMap;
    }

    public static <V> Map<String, V> prefixKeys(Map<String, V> oldMap) {
        Map<String, V> outMap = new LinkedHashMap<>();
        oldMap.forEach((k, v) -> {
            outMap.put("gv.".concat(k), v);
        });
        // Properties p = new Properties();
        // p.putAll(outMap);
        return outMap;
    }

    public static JsonElement fromProperties(Properties oldMap) {
        Properties map = new Properties();
        oldMap.forEach((k, v) -> {
            map.put(StringUtils.stripStart(String.valueOf(k), "gv."), v);
        });
        JsonElement json = gson.toJsonTree(map, Properties.class);
        return json;
        // gson.fromJson(json, CONFIG_BASE_TYPE_TOKEN.getType());
    }

    public static JsonElement fromMapx(Map<String, Object> oldMap) {
        Map<String, Object> map = shortenKeys(oldMap);
        JsonElement json = gson.toJsonTree(map, MAP_TYPE_TOKEN.getType());
        return json;
        // gson.fromJson(json, CONFIG_BASE_TYPE_TOKEN.getType());
    }
}
