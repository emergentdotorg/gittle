package org.emergent.maven.gitver.core;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import org.emergent.maven.gitver.core.version.PatternStrategy;
import org.emergent.maven.gitver.core.version.ResolvedData;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GsonUtil {

    private static final Map<Boolean, GsonUtil> INSTANCES = new ConcurrentHashMap<>();

    private static final TypeToken<List<Object>> LIST_TT = new TypeToken<>() {
    };
    private static final TypeToken<Map<Object, Object>> OBJ_OBJ_MAP_TT = new TypeToken<>() {
    };
    private static final TypeToken<Map<String, String>> STR_STR_MAP_TT = new TypeToken<>() {
    };
    public static final TypeToken<Map<String, Object>> STR_OBJ_MAP_TT = new TypeToken<>() {
    };
    private static final Predicate<String> IS_INT = Pattern.compile("^[0-9]+$").asMatchPredicate();

    @Getter
    private final boolean pretty;
    private final Gson gson;

    public static GsonUtil getInstance() {
        return getInstance(false);
    }

    public static GsonUtil getInstance(boolean pretty) {
        return INSTANCES.computeIfAbsent(pretty, GsonUtil::new);
    }

    private GsonUtil(boolean pretty) {
        this.pretty = pretty;
        GsonBuilder builder = getGsonBuilder(Map.of(
//                Properties.class, PropertiesGsonAdapter.INSTANCE,
//                PatternStrategy.class, PatternStrategyGsonAdapter.INSTANCE
        ));
        if (pretty) {
            builder.setPrettyPrinting();
        }
        this.gson = builder.create();
    }

    public Map<String, Object> toMapTree(Object src) {
        JsonElement tree = gson.toJsonTree(src, src.getClass());
        return gson.fromJson(tree, STR_OBJ_MAP_TT.getType());
    }

    public Map<String, Object> toSortedMapTree(Object src) {
        Map<String, String> flattened = flatten(src);
        Map<String, String> sorted = new TreeMap<>(flattened);
        JsonElement tree = gson.toJsonTree(sorted, STR_STR_MAP_TT.getType());
        return gson.fromJson(tree, STR_OBJ_MAP_TT.getType());
    }

    public Map<String, String> flatten(Object src) {
        return flatten(src, src.getClass());
    }

    public Map<String, String> flatten(Object src, Type type) {
        JsonElement json = flatten(gson.toJsonTree(src, type));
        return gson.fromJson(json, STR_STR_MAP_TT.getType());
    }

    public <V> V rebuild(Map<String, ?> in, Type type) {
        JsonElement json = gson.toJsonTree(in, STR_OBJ_MAP_TT.getType());
        JsonElement rebuilt = rebuild(json);
        return gson.fromJson(rebuilt, type);
    }

    private static JsonElement flatten(JsonElement src) {
        if (src.isJsonArray() || src.isJsonObject()) {
            JsonObject dst = new JsonObject();
            flatten(dst, "", src);
            return dst;
        }
        return src;
    }

    private static void flatten(JsonObject dst, String prefix, JsonElement in) {
        if (in.isJsonNull()) {
            return;
        }
        if (in.isJsonPrimitive()) {
            dst.add(prefix, in);
        }
        String prefixWithDot = prefix.isEmpty() ? "" : prefix + ".";
        if (in.isJsonObject()) {
            in.getAsJsonObject().asMap()
                    .forEach((k, v) -> flatten(dst, prefixWithDot + k, v));
        }
        if (in.isJsonArray()) {
            JsonArray arr = in.getAsJsonArray();
            IntStream.range(0, arr.size())
                    .forEach(i -> flatten(dst, prefixWithDot + (i + 1), arr.get(i)));
        }
    }

    private static JsonElement rebuild(JsonElement in) {
        if (!in.isJsonObject() || in.getAsJsonObject().keySet().stream().noneMatch(k -> k.contains("."))) {
            return in;
        }
        JsonObject jobj = in.getAsJsonObject();
        List<String> dottedKeys = jobj.keySet().stream().filter(k -> k.contains(".")).toList();

        dottedKeys.stream().map(k -> Util.substringBefore(k, "."))
                .filter(groupKey -> !(jobj.has(groupKey) && jobj.get(groupKey).isJsonObject()))
                .forEach(groupKey -> jobj.add(groupKey, new JsonObject()));

        dottedKeys.forEach(key -> {
            String groupKey = Util.substringBefore(key, ".");
            String subKey = Util.substringAfter(key, ".");
            JsonObject groupObj = jobj.getAsJsonObject(groupKey);
            JsonElement dottedValue = jobj.remove(key);
            groupObj.add(subKey, rebuild(dottedValue));
        });

        jobj.keySet().stream().filter(k -> jobj.get(k).isJsonObject()).toList().forEach(k -> {
            JsonObject groupObj = jobj.getAsJsonObject(k);
            List<String> intKeys = IntStream.range(1, groupObj.size() + 1).boxed()
                    .map(String::valueOf)
                    .filter(groupObj::has)
                    .toList();
            if (intKeys.size() == groupObj.size()) {
                JsonArray arr = new JsonArray();
                intKeys.forEach(ii -> arr.add(groupObj.get(ii)));
                jobj.add(k, arr);
            }
        });
        return jobj;
    }

    private static GsonBuilder getGsonBuilder(Map<Class<?>, Object> typeAdapters) {
        GsonBuilder builder = new GsonBuilder()
//                .setFieldNamingStrategy(MyFieldNamingStrategy.INSTANCE)
                .registerTypeAdapter(GitverConfig.class, new GitverConfigGsonAdapter())
                .registerTypeAdapter(ResolvedData.class, new ResolvedDataGsonAdapter())
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .setNumberToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE);
        typeAdapters.forEach(builder::registerTypeAdapter);
        return builder;
    }

    private static class GitverConfigGsonAdapter implements InstanceCreator<GitverConfig> {
        @Override
        public GitverConfig createInstance(Type type) {
            return GitverConfig.builder().build();
        }
    }

    private static class ResolvedDataGsonAdapter implements InstanceCreator<ResolvedData> {
        @Override
        public ResolvedData createInstance(Type type) {
            return ResolvedData.builder().build();
        }
    }

    public static class PatternStrategyGsonAdapter implements
//            InstanceCreator<PatternStrategy>,
            JsonSerializer<PatternStrategy>,
            JsonDeserializer<PatternStrategy>
    {
        public static final PatternStrategyGsonAdapter INSTANCE = new PatternStrategyGsonAdapter();

        private static final Map<String, String> NAME_REPL_MAP = Map.of("config", "gittle");
        private static final Map<String, String> REPL_NAME_MAP = Util.getReversed(NAME_REPL_MAP);
        private static final String STANDARD_PREFIX = "gittle_resolved_";
        private static final String VERSION_STRING = "versionString";

        private final Gson gson = getGsonBuilder(Map.of()).create();

//        @Override
//        public PatternStrategy createInstance(Type type) {
//            return PatternStrategy.create();
//        }

        @Override
        public JsonElement serialize(PatternStrategy src, Type type, JsonSerializationContext ctx) {
            JsonElement json = gson.toJsonTree(src, type);
//            if (json.isJsonObject()) {
//                JsonObject jobj = json.getAsJsonObject();
//                jobj.addProperty(VERSION_STRING, src.toVersionString());
//                jobj.keySet().stream().toList().forEach(key -> {
//                    String repl = NAME_REPL_MAP.getOrDefault(key, STANDARD_PREFIX + key);
//                    jobj.add(repl, jobj.remove(key));
//                });
//            }
            return json;
        }

        @Override
        public PatternStrategy deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
                throws JsonParseException {
//            if (json.isJsonObject()) {
//                JsonObject jobj = json.getAsJsonObject();
//                jobj.keySet().stream().toList().forEach(k -> {
//                    String name = REPL_NAME_MAP.getOrDefault(k, substringAfter(k, STANDARD_PREFIX));
//                    jobj.add(name, jobj.remove(k));
//                });
//                jobj.remove(VERSION_STRING);
//            }
            return gson.fromJson(json, type);
        }
    }

    public static class PropertiesGsonAdapter implements JsonSerializer<Properties>, JsonDeserializer<Properties> {

        public static final PropertiesGsonAdapter INSTANCE = new PropertiesGsonAdapter();

        private final Gson gson;

        public PropertiesGsonAdapter() {
            this.gson = getGsonBuilder(Map.of()).create();
        }

        @Override
        public Properties deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
                throws JsonParseException {
            Map<String, String> map = ctx.deserialize(json, STR_STR_MAP_TT.getType());
            Properties props = new Properties();
            props.putAll(map);
            return props;
        }

        @Override
        public JsonElement serialize(Properties src, Type type, JsonSerializationContext ctx) {
            return gson.toJsonTree(src, STR_STR_MAP_TT.getType());
        }
    }

    private static class MyFieldNamingStrategy implements FieldNamingStrategy {

        @Override
        public String translateName(Field f) {
            if (f.getDeclaringClass().equals(GitverConfig.class)) {
                return "gitver_" + f.getName();
            }
            if (f.getDeclaringClass().equals(PatternStrategy.class)) {
                return "gv_" + f.getName();
            }
            return f.getName();
        }

        @Override
        public List<String> alternateNames(Field f) {
            List<String> altnames = FieldNamingStrategy.super.alternateNames(f);
            if (f.getDeclaringClass().equals(PatternStrategy.class)) {
                altnames = new ArrayList<>(altnames);
                altnames.add("gv_" + f.getName());
            }
            return altnames.stream().sorted().distinct().collect(Collectors.toList());
        }
    }
}
