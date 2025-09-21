package org.emergent.maven.gitver.core;

import lombok.extern.java.Log;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.emergent.maven.gitver.core.version.ResolvedData;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.emergent.maven.gitver.core.Constants.GITTLE_PREFIX;
import static org.emergent.maven.gitver.core.Constants.GITTLE_RESOLVED_PREFIX;

@Log
public class PropCodec {

    public interface Codable<T extends Codable<T>> {

    }

    private static final GsonUtil gsonUtil = GsonUtil.getInstance();

    private static final Map<Class<?>, Map<String, String>> defaultsCache = new ConcurrentHashMap<>();

    private static final Map<Class<?>, String> TYPE_TO_PREFIX = Map.of(
            ResolvedData.class, GITTLE_RESOLVED_PREFIX,
            GitverConfig.class, GITTLE_PREFIX
    );

    public static <V extends Codable<V>> V fromProperties(Map<String, String> props,
                                                          Class<? extends Codable<V>> type) {
        String prefix = TYPE_TO_PREFIX.getOrDefault(type, "");
        Map<String, String> normalized = Util.removePrefix(prefix, props);
        normalized.keySet().stream().toList().stream()
                .filter(k -> StringUtils.countMatches(k, ".") > 0)
                .forEachOrdered(normalized::remove);
        return gsonUtil.rebuild(normalized, type);
    }

    public static <T extends Codable<T>> Map<String, String> toProperties(T src) {
        String prefix = TYPE_TO_PREFIX.getOrDefault(src.getClass(), "");
        Map<String, String> defmap = getDefs(src);
        Map<String, String> flattened = flatten(src);
        return flattened.entrySet().stream()
                .filter(e -> !Objects.equals(e.getValue(), defmap.get(e.getKey())))
                .map(e -> Map.entry(prefix + e.getKey(), e.getValue()))
                .collect(CollectorsEx.toTreeMap());
    }

    private static <T extends Codable<T>> Map<String, String> getDefs(T src) {
        return defaultsCache.computeIfAbsent(src.getClass(), c -> {
            T inst = gsonUtil.rebuild(Map.of(), c);
            return PropCodec.flatten(inst);
        });
    }

    private static <T extends Codable<T>> Map<String, String> flatten(T src) {
        return gsonUtil.flatten(Objects.requireNonNull(src, "src is null"));
    }

    static Map<String, String> flattenMap(Map<String, ?> src) {
        if (src.values().stream().noneMatch(v -> (v instanceof Map) || (v instanceof List))) {
            return src.entrySet().stream()
                    .map(e -> Map.entry(String.valueOf(e.getKey()), String.valueOf(e.getValue())))
                    .collect(CollectorsEx.toLinkedHashMap());
        }
        return gsonUtil.flattenMap(src);
    }

//    public static <T extends Codable<T>> Map<String, String> toProperties(T src, T def) {
//        Map<String, String> defmap = Optional.ofNullable(def)
//                .map(PropCodec::toProperties)
//                .orElseGet(Collections::emptyMap);
//        return toProperties(src).entrySet().stream()
//                .filter(e -> !Objects.equals(e.getValue(), defmap.get(e.getKey())))
//                .collect(CollectorsEx.toMap());
//    }

    public static Xpp3Dom toXml(GitverConfig src) {
        // Xpp3Dom dom = Xpp3DomBuilder.build(new StringReader(config.toXmlString()));
        Map<String, Object> map = gsonUtil.toMapTree(src);
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
