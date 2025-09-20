package org.emergent.maven.gitver.core;

import com.google.common.collect.Maps;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
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
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.emergent.maven.gitver.core.version.PatternStrategy;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
public class GsonUtil {

    private static final Map<Boolean, GsonUtil> INSTANCES = new ConcurrentHashMap<>();

    private static final TypeToken<List<Object>> LIST_TT = new TypeToken<>() {
    };
    private static final TypeToken<Map<Object, Object>> OBJ_OBJ_MAP_TT = new TypeToken<>() {
    };
    private static final TypeToken<Map<String, String>> STR_STR_MAP_TT = new TypeToken<>() {
    };
    private static final TypeToken<Map<String, Object>> STR_OBJ_MAP_TT = new TypeToken<>() {
    };
    private static final TypeToken<PatternStrategy> PATTERN_STRATEGY_TT = new TypeToken<>() {
    };
    private static final TypeToken<GitverConfig> GITVER_CONFIG_TT = new TypeToken<>() {
    };

    private static Map<Class<?>, Object> getDefaultTypeAdapters() {
        return Map.of(
                List.class, ListDeserializer.INSTANCE,
                Map.class, MapDeserializer.INSTANCE,
                Properties.class, PropertiesGsonAdapter.INSTANCE,
                GitverConfig.class, GitverConfigurationGsonAdapter.INSTANCE,
                PatternStrategy.class, PatternStrategyGsonAdapter2.INSTANCE
        );
    }

    public static GsonUtil getInstance() {
        return getInstance(false);
    }

    public static GsonUtil getInstance(boolean pretty) {
        return INSTANCES.computeIfAbsent(pretty, GsonUtil::new);
    }

    private final Gson gson;

    private GsonUtil(boolean pretty) {
        this(pretty, getDefaultTypeAdapters());
    }

    private GsonUtil(boolean pretty, Map<Class<?>, Object> typeAdapters) {
        GsonBuilder builder = new GsonBuilder();
        if (pretty) {
            builder.setPrettyPrinting();
        }
        typeAdapters.forEach(builder::registerTypeAdapter);
        gson = builder
                .setExclusionStrategies(new MyExclusionStrategy())
                .setFieldNamingStrategy(new MyFieldNamingStrategy())
                .create();
    }

    public <V> V fromJson(JsonElement src, Type type) {
        return gson.fromJson(src, type);
    }

    public JsonObject toJsonTree(Map<String, ?> src) {
        return gson.toJsonTree(new LinkedHashMap<>(src), STR_OBJ_MAP_TT.getType()).getAsJsonObject();
    }

    public Map<String, Object> toObjectMap(JsonObject src) {
        return gson.fromJson(src, STR_OBJ_MAP_TT.getType());
    }

    public Map<String, String> toStringMap(JsonObject src) {
        return gson.fromJson(src, STR_STR_MAP_TT.getType());
    }

    public Object unwrap(JsonElement src) {
        return BasicCodec.extract(src);
    }

    public JsonObject toDotted(JsonElement in) {
        JsonObject dest = new JsonObject();
        toDotted(dest, "", in);
        return dest;
    }

    private void toDotted(JsonObject dest, String prefix, JsonElement in) {
        if (in.isJsonNull() || in.isJsonPrimitive()) {
            dest.add(prefix, in);
        }
        String prefixWithDot = prefix.isEmpty() ? "" : prefix + ".";
        if (in.isJsonObject()) {
            in.getAsJsonObject().asMap()
                    .forEach((k, v) -> toDotted(dest, prefixWithDot + k, v));
        }
        if (in.isJsonArray()) {
            JsonArray arr = in.getAsJsonArray();
            IntStream.range(0, arr.size())
                    .forEach(i -> toDotted(dest, prefixWithDot + (i+1), arr.get(i)));
        }
    }

    public Map<String, Object> toUndotted(Map<String, ?> in) {
        JsonElement el = toJsonTree(new LinkedHashMap<String, Object>(in));
        JsonElement transformed = toUndotted(el);
        return toObjectMap(transformed.getAsJsonObject());
    }

    public JsonElement toUndotted(JsonElement in) {
        if (!in.isJsonObject() && !in.isJsonArray()) {
            return in;
        }

        if (in.isJsonObject() && in.getAsJsonObject().keySet().stream().anyMatch(k -> k.contains("."))) {
            JsonObject out = new JsonObject();

            in.getAsJsonObject().asMap().forEach((k, v) -> {
                if (k.contains(".")) {
                    String grpkey = StringUtils.substringBefore(k, ".");
                    String subkey = StringUtils.substringAfter(k, ".");
                    JsonObject grpmap = Optional.ofNullable(out.get(grpkey))
                            .filter(j -> j.isJsonObject())
                            .map(j -> j.getAsJsonObject())
                            .orElseGet(() -> {
                                JsonObject o = new JsonObject();
                                out.add(grpkey, o);
                                return o;
                            });

                    grpmap.add(subkey, toUndotted(v));
                } else {
                    out.add(k, v);
                }
            });

            LinkedHashSet<String> outkeys = new LinkedHashSet<>(out.keySet());

            outkeys.forEach(k -> Optional.ofNullable(out.get(k))
                    .filter(JsonElement::isJsonObject)
                    .map(JsonElement::getAsJsonObject)
                    // .filter(jsonObj -> !jsonObj.isJsonArray())
                    .ifPresent(v -> {
                        Set<String> keys = new HashSet<>(v.keySet());
                        JsonArray arr = new JsonArray();
                        IntStream.range(1, keys.size() + 1).boxed().forEach(ii -> {
                            JsonElement item = v.get(Integer.toString(ii));
                            if (item != null) {
                                arr.add(item);
                            }
                        });
                        if (arr.size() == keys.size()) {
                            out.add(k, arr);
                        }
                    }));

            return out;
        }
        return in;
    }

    public static class GitverConfigurationGsonAdapter implements InstanceCreator<GitverConfig> {
        public static final GitverConfigurationGsonAdapter INSTANCE = new GitverConfigurationGsonAdapter();

        @Override
        public GitverConfig createInstance(Type type) {
            return GitverConfig.builder().build();
        }
    }

    public static class PatternStrategyGsonAdapter
            implements InstanceCreator<PatternStrategy>
    {
        public static final PatternStrategyGsonAdapter INSTANCE = new PatternStrategyGsonAdapter();

        @Override
        public PatternStrategy createInstance(Type type) {
            return PatternStrategy.builder().build();
        }
    }

    public static class PatternStrategyGsonAdapter2
            implements InstanceCreator<PatternStrategy>,
           JsonSerializer<PatternStrategy>,
            JsonDeserializer<PatternStrategy>
    {
        public static final PatternStrategyGsonAdapter2 INSTANCE = new PatternStrategyGsonAdapter2();

        private final Gson gson;

        public PatternStrategyGsonAdapter2() {
            GsonBuilder builder = new GsonBuilder()
                    .setExclusionStrategies(new MyExclusionStrategy())
                    .setFieldNamingStrategy(new MyFieldNamingStrategy())
                    .setPrettyPrinting();
            Map.of(
                    List.class, ListDeserializer.INSTANCE,
                    Map.class, MapDeserializer.INSTANCE,
                    Properties.class, PropertiesGsonAdapter.INSTANCE,
                    GitverConfig.class, GitverConfigurationGsonAdapter.INSTANCE,
                    PatternStrategy.class, PatternStrategyGsonAdapter.INSTANCE
            ).forEach(builder::registerTypeAdapter);
            gson = builder.create();
        }

        @Override
        public PatternStrategy createInstance(Type type) {
            return PatternStrategy.builder().build();
        }

        @Override
        public JsonElement serialize(PatternStrategy src, Type type, JsonSerializationContext ctx) {
            JsonElement json = gson.toJsonTree(src, type);
            if (json.isJsonObject()) {
                json.getAsJsonObject().addProperty("gv.versionString", src.toVersionString());
            }
            return json;
        }

        @Override
        public PatternStrategy deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
                throws JsonParseException {
            if (json.isJsonObject()) {
                json.getAsJsonObject().remove("gv.versionString");
            }
            return gson.fromJson(json, type);
        }
    }

    public static class PropertiesGsonAdapter implements JsonSerializer<Properties>, JsonDeserializer<Properties> {

        public static final PropertiesGsonAdapter INSTANCE = new PropertiesGsonAdapter();

        @Override
        public Properties deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
                throws JsonParseException {
            Map<String, String> map = ctx.deserialize(PropCodec.getInstance().toDotted(json), STR_STR_MAP_TT.getType());
            Properties props = new Properties();
            props.putAll(map);
            return props;
        }

        @Override
        public JsonElement serialize(Properties src, Type type, JsonSerializationContext ctx) {
            Map<String, String> map = src.entrySet().stream()
                    .filter(e -> e.getKey() instanceof String && e.getValue() instanceof String)
                    .map(e -> Map.entry(String.valueOf(e.getKey()), String.valueOf(e.getValue())))
                    .collect(CollectorsEx.toMap(LinkedHashMap::new));
            GsonUtil util = getInstance();
            return util.toUndotted(ctx.serialize(util.toUndotted(map), STR_STR_MAP_TT.getType()));
        }
    }

    private static class MapDeserializer extends BasicCodec implements JsonDeserializer<Map<String, Object>> {

        public static final MapDeserializer INSTANCE = new MapDeserializer();

        public Map<String, Object> deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
                throws JsonParseException {
            return extractMap(json.getAsJsonObject());
        }
    }

    private static class ListDeserializer extends BasicCodec implements JsonDeserializer<List<Object>> {

        public static final ListDeserializer INSTANCE = new ListDeserializer();

        public List<Object> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
                throws JsonParseException {
            return extractList(json.getAsJsonArray());
        }
    }

    @Log
    public static class BasicCodec {

        public static Object extract(JsonElement json) {
            if (json.isJsonNull()) {
                return null;
            } else if (json.isJsonArray()) {
                return extractList(json.getAsJsonArray());
            } else if (json.isJsonObject()) {
                return extractMap(json.getAsJsonObject());
            } else {
                return extractPrimitive(json.getAsJsonPrimitive());
            }
        }

        public static List<Object> extractList(JsonArray array) {
            return array.asList().stream().map(v -> {
                String typeName = v.isJsonArray()
                        ? "array" : (v.isJsonObject()
                        ? "object" : (v.isJsonPrimitive()
                        ? "primitive"
                        : "null"));
                Object val = switch (typeName) {
                    case "array" -> extractList(v.getAsJsonArray());
                    case "object" -> extractMap(v.getAsJsonObject());
                    case "primitive" -> extractPrimitive(v.getAsJsonPrimitive());
                    default -> null;
                };
                return val;
            }).toList();
        }

        public static Map<String, Object> extractMap(JsonObject object) {
            return object.asMap().entrySet().stream().map(e -> {
                String k = e.getKey();
                JsonElement v = e.getValue();
                String typeName = v.isJsonArray()
                        ? "array" : (v.isJsonObject()
                        ? "object" : (v.isJsonPrimitive()
                        ? "primitive"
                        : "null"));
                Object val = switch (typeName) {
                    case "array" -> extractList(v.getAsJsonArray());
                    case "object" -> extractMap(v.getAsJsonObject());
                    case "primitive" -> extractPrimitive(v.getAsJsonPrimitive());
                    default -> null;
                };
                return Map.entry(e.getKey(), val);
            }).collect(CollectorsEx.toMap(LinkedHashMap::new));
        }

        public static Object extractPrimitive(JsonPrimitive v) {
            if (v.isJsonNull()) {
                return null;
            } else if (v.isBoolean()) {
                return v.getAsBoolean();
            } else if (v.isNumber()) {
                try {
                    return Optional.of(v)
                            .filter(JsonElement::isJsonPrimitive)
                            .map(JsonElement::getAsJsonPrimitive)
                            .filter(JsonPrimitive::isNumber)
                            .map(JsonElement::toString)
                            .map(BigDecimal::new)
                            .map(BigDecimal::stripTrailingZeros)
                            .map(bd -> {
                                if (bd.scale() <= 0) {
                                    return bd.toBigInteger().intValueExact();
                                } else {
                                    return bd.doubleValue();
                                }
                            })
                            .orElseGet(() -> v.getAsNumber().longValue());
                } catch (Exception e) {
                    // log.log(Level.WARNING, "Could not parse " + v.getAsJsonPrimitive().getAsString(), e);
                    return v.getAsNumber();
                }
            } else {
                String str = v.getAsString();
                try {
                    if (java.util.Set.of("true", "false").contains(str)) {
                        return Boolean.parseBoolean(str);
                    }
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed parsing string as a boolean " + str, e);
                }
                try {
                    if (str.matches("^([-])?[0-9]+$")) {
                        return Long.parseLong(str);
                    } else if (str.matches("^([-])?[0-9]+\\.[0-9]+$")) {
                        return Double.parseDouble(str);
                    }
                } catch (Exception e) {
                    log.log(Level.WARNING, "Failed parsing string as a number " + str, e);
                }
                return str;
            }
        }
    }

    private static class MyFieldNamingStrategy implements FieldNamingStrategy {

        @Override
        public String translateName(Field f) {
//            if (f.getDeclaringClass().equals(PatternStrategy.class)) {
//                return "gv." + f.getName();
//            }
            return f.getName();
        }

        @Override
        public List<String> alternateNames(Field f) {
            List<String> altnames = FieldNamingStrategy.super.alternateNames(f);
//            if (f.getDeclaringClass().equals(PatternStrategy.class)) {
//                altnames = new ArrayList<>(altnames);
//                altnames.add("gv." + f.getName());
//            }
            return altnames.stream().sorted().distinct().collect(Collectors.toList());
        }
    }

    private static class MyExclusionStrategy implements ExclusionStrategy {

        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            // if (ConversionHelper.class.isAssignableFrom(f.getDeclaringClass())) {
            //     return Set.of("conversionHelper", "getConversionHelper").contains(f.getName());
            // }
            return false;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }
}
