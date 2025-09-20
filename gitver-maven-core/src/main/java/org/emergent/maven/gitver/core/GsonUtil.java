package org.emergent.maven.gitver.core;

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
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collections;
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
import java.util.logging.Level;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.emergent.maven.gitver.core.version.PatternStrategy;

@Getter
public class GsonUtil {

    private static final Map<Boolean, GsonUtil> INSTANCES = new ConcurrentHashMap<>();

    private static final TypeToken<List<Object>> LIST_TT = new TypeToken<>() {};
    private static final TypeToken<Map<Object, Object>> OBJ_OBJ_MAP_TT = new TypeToken<>() {};
    private static final TypeToken<Map<String, String>> STR_STR_MAP_TT = new TypeToken<>() {};
    private static final TypeToken<Map<String, Object>> STR_OBJ_MAP_TT = new TypeToken<>() {};
    private static final TypeToken<PatternStrategy> PATTERN_STRATEGY_TT = new TypeToken<>() {};
    private static final TypeToken<GitverConfig> GITVER_CONFIG_TT = new TypeToken<>() {};

    public static GsonUtil getInstance() {
        return getInstance(false);
    }

    public static GsonUtil getInstance(boolean pretty) {
        return INSTANCES.computeIfAbsent(pretty, GsonUtil::new);
    }

    private final Gson gson;

    private GsonUtil(boolean pretty) {
        GsonBuilder builder = new GsonBuilder();
        if (pretty) {
            builder.setPrettyPrinting();
        }
        gson = builder
                .setExclusionStrategies(new MyExclusionStrategy())
                .setFieldNamingStrategy(new MyFieldNamingStrategy())
                .registerTypeAdapter(List.class, new ListDeserializer())
                .registerTypeAdapter(Map.class, new MapDeserializer())
                .registerTypeAdapter(Properties.class, new PropertiesGsonAdapter())
                .registerTypeAdapter(GitverConfig.class, new GitverConfigurationGsonAdapter())
                .registerTypeAdapter(PatternStrategy.class, new PatternStrategyGsonAdapter())
                .create();
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
        if (in.isJsonObject() || in.isJsonArray()) {
            Map<String, JsonElement> map = Collections.emptyMap();
            if (in.isJsonObject()) {
                map = in.getAsJsonObject().asMap();
            } else if (in.isJsonArray()) {
                AtomicInteger idx = new AtomicInteger();
                map = in.getAsJsonArray().asList().stream()
                  .map(v -> Map.entry("" + idx.incrementAndGet(), v))
                  .collect(CollectorsEx.toMap(LinkedHashMap::new));
            }
            String prefixWithDot = prefix.isEmpty() ? "" : prefix + ".";
            map.forEach((k, v) -> {
                toDotted(dest, prefixWithDot + k, v);
            });
        } else {
            dest.add(prefix, in);
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

        @Override
        public GitverConfig createInstance(Type type) {
            return GitverConfig.builder().build();
        }
    }

    public static class PatternStrategyGsonAdapter implements InstanceCreator<PatternStrategy> {
        @Override
        public PatternStrategy createInstance(Type type) {
            return PatternStrategy.builder().build();
        }
    }

    public static class PropertiesGsonAdapter implements JsonSerializer<Properties>, JsonDeserializer<Properties> {
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

        public Map<String, Object> deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
                throws JsonParseException {
            return extractMap(json.getAsJsonObject());
        }
    }

    private static class ListDeserializer extends BasicCodec implements JsonDeserializer<List<Object>> {
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
                                    return bd.toBigInteger().longValueExact();
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
                    if (Set.of("true", "false").contains(str)) {
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
            return f.getName();
        }

        @Override
        public List<String> alternateNames(Field f) {
            return FieldNamingStrategy.super.alternateNames(f);
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
