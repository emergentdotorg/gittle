package org.emergent.maven.gitver.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import lombok.Value;

@Value
public class MapperEx {

    public static final Set<Class<?>> PRIMITIVE_TYPES = Set.of(Boolean.class, String.class, Integer.class);
    public static final Set<Class<?>> ALLOWED_TYPES = new HashSet<>(Set.of(List.class, Map.class));

    static {
        ALLOWED_TYPES.addAll(PRIMITIVE_TYPES);
    }

    Map<String, Object> map = new TreeMap<>();
    Map<String, Object> def;

    private MapperEx() {
        this(new TreeMap<>());
    }

    private MapperEx(Map<String, Object> def) {
        this.def = def;
    }

    public static MapperEx create() {
        return create(new TreeMap<>());
    }

    public static MapperEx create(Map<String, Object> def) {
        return new MapperEx(def);
    }

    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(map);
    }

    public Map<String, String> toFlattened() {
        return Util.flatten(toMap());
    }

    public Properties toProperties() {
        Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }

    public MapperEx putAll(Map<String, Object> map) {
        map.forEach(this::put);
        return this;
    }

    public MapperEx putAll(Properties properties) {
        Util.flatten(properties).forEach(this::put);
        return this;
    }

    public MapperEx put(String key, boolean value) {
        return put(key, value, def.getOrDefault(key, false));
    }

    public MapperEx put(String key, int value) {
        return put(key, value, def.getOrDefault(key, 0));
    }

    public MapperEx put(String key, Object value) {
        return put(key, value, def.get(key));
    }

    public MapperEx put(String key, boolean value, boolean def) {
        return put(key, (Object) value, (Object) def);
    }

    public MapperEx put(String key, int value, int def) {
        return put(key, (Object) value, (Object) def);
    }

    public MapperEx put(String key, Object value, Object def) {
        Optional.ofNullable(value)
                .filter(v -> ALLOWED_TYPES.stream().anyMatch(c -> c.isAssignableFrom(v.getClass())))
                .filter(v -> !Objects.equals(v, def))
                .ifPresent(v -> map.put(key, v));
        return this;
    }
}
