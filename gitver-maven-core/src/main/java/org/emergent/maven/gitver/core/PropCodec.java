package org.emergent.maven.gitver.core;

import lombok.extern.java.Log;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

@Log
public class PropCodec {

    public interface Codable<T extends Codable<T>> {

    }

    private static final GsonUtil gsonUtil = GsonUtil.getInstance();

    public static <V extends Codable<V>> V fromProperties(Properties props, Class<? extends Codable<V>> type) {
        return fromProperties(Util.toStringStringMap(props), type);
    }

    public static <V extends Codable<V>> V fromProperties(Map<String, String> props,
                                                          Class<? extends Codable<V>> type) {
        return gsonUtil.rebuild(props, type);
    }

    public static <T extends Codable<T>> Map<String, String> toProperties(T src) {
        Map<String, String> defmap = Optional.ofNullable(acquireDefault(src))
                .map(PropCodec::toProperties0)
                .orElseGet(Collections::emptyMap);
        return toProperties0(src).entrySet().stream()
                .filter(e -> !Objects.equals(e.getValue(), defmap.get(e.getKey())))
                .collect(CollectorsEx.toMap());
    }

    private static <T extends Codable<T>> Map<String, String> toProperties0(T src) {
        Objects.requireNonNull(src, "src is null");
        return gsonUtil.flatten(src);
    }

    public static <T extends Codable<T>> Map<String, String> toProperties(T src, T def) {
        Map<String, String> defmap = Optional.ofNullable(def)
                .map(PropCodec::toProperties)
                .orElseGet(Collections::emptyMap);
        return toProperties(src).entrySet().stream()
                .filter(e -> !Objects.equals(e.getValue(), defmap.get(e.getKey())))
                .collect(CollectorsEx.toMap());
    }

    public static Xpp3Dom toXml(GitverConfig src) {
        // Xpp3Dom dom = Xpp3DomBuilder.build(new StringReader(config.toXmlString()));
        Map<String, Object> map = gsonUtil.toSortedMapTree(src);
        return toXml("configuration", map);
    }

    public static Xpp3Dom toXml(String name, Object value) {
        Xpp3Dom dom = new Xpp3Dom(name);
        if (value instanceof Map<?, ?> m) {
            toXml(m).forEach(dom::addChild);
        } else if (value != null) {
            dom.setValue(String.valueOf(value));
        }
        return dom;
    }

    public static List<Xpp3Dom> toXml(Map<?, ?> map) {
        return map.entrySet().stream()
                .filter(e -> e.getKey() instanceof String)
                .filter(e -> Objects.nonNull(e.getValue()))
                .map(e -> toXml((String)e.getKey(), e.getValue()))
                .toList();
    }

    private static <T extends Codable<T>> T acquireDefault(T src) {
        return gsonUtil.rebuild(Map.of(), src.getClass());
//        try {
//            Object builder = src.getClass().getDeclaredMethod("builder").invoke(null);
//            return (T)builder.getClass().getDeclaredMethod("build").invoke(builder);
//        } catch (ReflectiveOperationException e) {
//            throw new GitverException(e.getMessage(), e);
//        }
    }

}
