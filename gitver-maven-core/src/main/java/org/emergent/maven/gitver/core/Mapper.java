package org.emergent.maven.gitver.core;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@SuppressWarnings("UnusedReturnValue")
public interface Mapper {

    static Mapper create() {
        return create("");
    }

    static Mapper create(String prefix) {
        return new MapperImpl(new TreeMap<>(), prefix);
    }

    Mapper putAll(Map<String, String> map);

    Mapper put(String key, boolean value);

    Mapper put(String key, boolean value, boolean def);

    Mapper put(String key, int value);

    Mapper put(String key, int value, int def);

    Mapper put(String key, String value);

    Mapper put(String key, String value, String def);

    Map<String, String> toMap();

    class MapperImpl implements Mapper {
        private final Map<String, String> map;
        private final String prefix;

        private MapperImpl(Map<String, String> map, String prefix) {
            this.map = map;
            this.prefix = prefix;
        }

        @Override
        public Map<String, String> toMap() {
            return Collections.unmodifiableMap(map);
        }

        @Override
        public Mapper putAll(Map<String, String> map) {
            this.map.putAll(map);
            return this;
        }

        @Override
        public Mapper put(String key, boolean value) {
            return put(key, value, false);
        }

        @Override
        public Mapper put(String key, boolean value, boolean def) {
            if (value != def) map.put(prefix + key, Boolean.toString(value));
            return this;
        }

        @Override
        public Mapper put(String key, int value) {
            return put(key, value, 0);
        }

        @Override
        public Mapper put(String key, int value, int def) {
            if (value != def) map.put(prefix + key, Integer.toString(value));
            return this;
        }

        @Override
        public Mapper put(String key, String value) {
            return put(key, value, "");
        }

        @Override
        public Mapper put(String key, String value, String def) {
            if (Util.isNotEmpty(value) && !Objects.equals(value, def)) map.put(prefix + key, value);
            return this;
        }
    }
}
