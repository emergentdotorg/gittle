package org.emergent.maven.gitver.core;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;

@Log
public class PropCodec {

    private static final PropCodec INSTANCE = new PropCodec();

    private static final GsonUtil gsonUtil = GsonUtil.getInstance();

    private PropCodec() {
    }

    public static PropCodec getInstance() {
        return INSTANCE;
    }

    private static Gson getGson() {
        return gsonUtil.getGson();
    }

    public <V> V fromProperties(Properties props, Type type) {
        return fromProperties(toStringStringMap(props), type);
    }

    public <V> V fromProperties(Map<String, String> props, Type type) {
        Map<String, Object> undotted = props.entrySet().stream()
          .collect(CollectorsEx.toMapAndThen(m -> toUndotted(m)));
        JsonElement json = getGson().toJsonTree(undotted, GsonUtil.STR_OBJ_MAP_TT.getType());
        return getGson().fromJson(json, type);
    }

    public <T> Map<String, String> toProperties(T src, T def, Type type) {
        Objects.requireNonNull(src, "src is null");
        Objects.requireNonNull(type, "type is null");

        Map<String, String> defmap = Optional.ofNullable(def)
          .map(d -> toMap(def, type)).orElseGet(Collections::emptyMap)
          .entrySet().stream().collect(toDottedCol());

        Map<String, String> srcmap = toMap(src, type).entrySet().stream()
          .collect(toDottedCol());

        return srcmap.entrySet().stream()
          .filter(e -> !(Objects.isNull(e.getValue()) || Objects.equals(e.getValue(), defmap.get(e.getKey()))))
          .collect(CollectorsEx.toMap());
    }

    private Collector<Entry<String, Object>, Map<String, Object>, Map<String, String>> toDottedCol() {
        return CollectorsEx.toMapAndThen(this::toDotted);
    }

    private static Map<String, Object> toMap(Object src, Type type) {
        if (src == null) {
            return Collections.emptyMap();
        }
        JsonElement json = getGson().toJsonTree(src, type);
        return getGson().fromJson(json, GsonUtil.STR_OBJ_MAP_TT.getType());
    }

    Map<String, String> toDotted(Map<String, ?> map) {
        JsonElement json = getGson().toJsonTree(map, GsonUtil.STR_OBJ_MAP_TT.getType());
        JsonObject dotted = toDotted(json);
        Map<String, Object> objmap = getGson().fromJson(dotted, GsonUtil.STR_OBJ_MAP_TT.getType());
        return objmap.entrySet().stream()
          .filter(e -> Allowed.isValuePrimitive(e))
          .map(e -> Map.entry(e.getKey(), String.valueOf(e.getValue())))
          .collect(CollectorsEx.toMap());
    }

    private Map<String, String> toDottedCollector(Map<String, ?> map) {
        JsonElement json = getGson().toJsonTree(map, GsonUtil.STR_OBJ_MAP_TT.getType());
        JsonObject dotted = toDotted(json);
        Map<String, Object> objmap = getGson().fromJson(dotted, GsonUtil.STR_OBJ_MAP_TT.getType());
        Collector<Entry<String, String>, ?, Map<String, String>> col = CollectorsEx.toMap();
        return objmap.entrySet().stream()
          .filter(e -> Allowed.isPrimitive(e))
          .map(e -> Map.entry(e.getKey(), String.valueOf(e.getValue())))
          .collect(col);
    }

    // public static <T extends Map.Entry<? extends K, ? extends V>, K, V> Collector<T, ?, Map<K, V>> toMap() {
    //     return toTreeMap();
    // }

    private static <T extends Entry<String, Object>> Collector<T, ?, Map<String, Object>> toMap2(
      Function<T, String> keyMapper,
      Function<T, Object> valMapper) {
        return CollectorsEx.toLinkedHashMap(keyMapper, valMapper);
    }

    public JsonObject toDotted(JsonElement in) {
        JsonObject dest = new JsonObject();
        toDotted(dest, "", in);
        return dest;
    }

    private JsonObject toDotted(String prefix, JsonElement in) {
        JsonObject dest = new JsonObject();
        toDotted(dest, prefix, in);
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
        JsonElement el = getGson().toJsonTree(new LinkedHashMap<String, Object>(in), GsonUtil.STR_OBJ_MAP_TT.getType());
        JsonElement transformed = toUndotted(el);
        return getGson().fromJson(transformed, GsonUtil.STR_OBJ_MAP_TT.getType());
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

    private static void log(Map<String, Object> outMap, Type typeOfSrc) {
        System.out.printf(
          "%s properties: %s%n",
          typeOfSrc.getTypeName(),
          outMap.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining("\n", "\n", "\n")));
    }

    public Map<String, String> toStringStringMap(Properties properties) {
        return toStringStringMap(properties.entrySet());
    }

    public Map<String, String> toStringStringMap(Map<?, ?> map) {
        return toStringStringMap(map.entrySet());
    }

    private static Map<String, String> toStringStringMap(Set<? extends Entry<?, ?>> entries) {
        return entries.stream()
          .filter(e -> e.getKey() instanceof String && e.getValue() instanceof String)
          .collect(CollectorsEx.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue())));
    }

    public static class Allowed {

        private static final Set<Class<?>> CONTAINER_TYPES =
          Set.of(Map.class, Iterable.class);

        private static final Set<Class<?>> FLOAT_TYPES =
          Set.of(Float.class, Double.class, BigDecimal.class);

        private static final Set<Class<?>> INTEGRAL_TYPES =
          Set.of(Byte.class, Short.class, Integer.class, Long.class, BigInteger.class);

        private static final Set<Class<?>> NUMBER_TYPES = Stream.concat(
          INTEGRAL_TYPES.stream(), FLOAT_TYPES.stream()
        ).collect(Collectors.toUnmodifiableSet());

        private static final Set<Class<?>> PRIMITIVE_TYPES = Stream.concat(
          Stream.of(Boolean.class, String.class), NUMBER_TYPES.stream()
        ).collect(Collectors.toUnmodifiableSet());

        public static boolean isKnownType(Object value) {
            return isAllowedValue0(getAllowedTypes(), value);
        }

        public static boolean isKnownValueType(Entry<?, ?> entry) {
            return isAllowedEntry0(getAllowedTypes(), entry);
        }

        public static boolean isPrimitive(Object value) {
            return isAllowedValue0(getPrimitiveTypes(), value);
        }

        public static boolean isValuePrimitive(Entry<?, ?> entry) {
            return isAllowedEntry0(getPrimitiveTypes(), entry);
        }

        public static boolean isContainer(Object value) {
            return isAllowedValue0(getContainerTypes(), value);
        }

        public static boolean isValueContainer(Entry<?, ?> entry) {
            return isAllowedEntry0(getContainerTypes(), entry);
        }

        private static boolean isAllowedValue0(Set<Class<?>> types, Object value) {
            return types.stream().anyMatch(t -> t.isInstance(value));
        }

        private static boolean isAllowedEntry0(Set<Class<?>> types, Entry<?, ?> entry) {
            return (entry.getKey() instanceof String) && isAllowedValue0(types, entry.getValue());
        }

        public static Set<Class<?>> getAllowedTypes() {
            Set<Class<?>> types = new HashSet<>();
            types.addAll(getPrimitiveTypes());
            types.addAll(getContainerTypes());
            return types;
        }

        public static Set<Class<?>> getContainerTypes() {
            return CONTAINER_TYPES;
        }

        public static Set<Class<?>> getPrimitiveTypes() {
            Set<Class<?>> types = new HashSet<>(PRIMITIVE_TYPES);
            types.addAll(getIntegralTypes());
            types.addAll(getFloatTypes());
            return Collections.unmodifiableSet(types);
        }

        public static Set<Class<?>> getIntegralTypes() {
            return INTEGRAL_TYPES;
        }

        public static Set<Class<?>> getFloatTypes() {
            return FLOAT_TYPES;
        }
    }
}
