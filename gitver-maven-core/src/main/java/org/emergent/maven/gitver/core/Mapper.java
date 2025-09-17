package org.emergent.maven.gitver.core;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;
import lombok.Value;

@Value
public class Mapper {

    private final Map<String, String> map;

    public Mapper(Map<String, String> map) {
        this.map = map;
    }

    public static Mapper create(Map<String, Object> def) {
        return new Mapper(new TreeMap<>());
    }

    public static Mapper create() {
        return new Mapper(new TreeMap<>());
    }

    public Map<String, String> toMap() {
        return Collections.unmodifiableMap(map);
    }

    public Properties toProperties() {
        Properties properties = new Properties();
        properties.putAll(map);
        return properties;
    }

    public Mapper putObj(String key, Object value, Object def) {
        if (value != def) map.put(key, String.valueOf(value));
        return this;
    }

    public Mapper putAll(Map<String, String> map) {
        this.map.putAll(map);
        return this;
    }

    public Mapper putAll(Properties properties) {
        return putAll(Util.flatten(properties));
    }

    public Mapper put(String key, boolean value) {
        return put(key, value, false);
    }

    public Mapper put(String key, int value) {
        return put(key, value, 0);
    }

    public Mapper put(String key, String value) {
        return put(key, value, "");
    }

    public Mapper put(String key, boolean value, boolean def) {
        if (value != def) map.put(key, Boolean.toString(value));
        return this;
    }

    public Mapper put(String key, int value, int def) {
        if (value != def) map.put(key, Integer.toString(value));
        return this;
    }

    public Mapper put(String key, String value, String def) {
        if (Util.isNotEmpty(value) && !Objects.equals(value, def)) map.put(key, value);
        return this;
    }

    public Map<String, String> map() {
        return map;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (Mapper) obj;
        return Objects.equals(this.map, that.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }

    @Override
    public String toString() {
        return "Mapper[" + "map=" + map + ']';
    }
}
