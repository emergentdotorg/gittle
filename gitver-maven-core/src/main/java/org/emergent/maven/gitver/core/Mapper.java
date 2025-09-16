package org.emergent.maven.gitver.core;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.TreeMap;

public record Mapper(Map<String, String> map) {

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
}
