package org.emergent.maven.gitver.core;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.java.Log;

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
        JsonElement json = gsonUtil.toJsonTree(undotted);
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
        try {
            JsonElement json = getGson().toJsonTree(src, type);
            return gsonUtil.toObjectMap(json.getAsJsonObject());
        } catch (Exception e) {
            log.log(Level.WARNING, "Could not convert " + src + " to Map", e);
            return Collections.emptyMap();
        }
    }

    Map<String, String> toDotted(Map<String, ?> map) {
        JsonElement json = getGson().toJsonTree(map);
        JsonObject dotted = toDotted(json);
        Map<String, Object> objmap = gsonUtil.toObjectMap(dotted);
        return objmap.entrySet().stream()
          .filter(e -> Allowed.isValuePrimitive(e))
          .map(e -> Map.entry(e.getKey(), String.valueOf(e.getValue())))
          .collect(CollectorsEx.toMap());
    }

    private Map<String, String> toDottedCollector(Map<String, ?> map) {
        JsonElement json = getGson().toJsonTree(map);
        JsonObject dotted = toDotted(json);
        Map<String, Object> objmap = gsonUtil.toObjectMap(dotted);
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
        return gsonUtil.toDotted(in);
    }

    public Map<String, Object> toUndotted(Map<String, ?> in) {
        return gsonUtil.toUndotted(in);
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
        return Util.toStringStringMap(properties);
    }

    public Map<String, String> toStringStringMap(Map<?, ?> map) {
        return Util.toStringStringMap(map);
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
