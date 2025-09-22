package org.emergent.maven.gitver.core;

import com.google.gson.Gson;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonElement;
import lombok.extern.java.Log;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static org.emergent.maven.gitver.core.GsonUtil.STR_STR_MAP_TT;

@Log
public class PropCodec {

    public interface Codable {

    }

    private static final GsonUtil gsonUtil = GsonUtil.getInstance();

    private static final Map<Class<?>, Map<String, String>> defaultsCache = new ConcurrentHashMap<>();

    private static final Map<Class<?>, String> TYPE_TO_PREFIX = GsonUtil.MyFieldNamingStrategy.PREFIX_MAP
            .entrySet().stream()
            .map(e -> Map.entry(e.getKey(), e.getValue().replace('_', '.')))
            .collect(CollectorsEx.toMap());

    private static final Gson rawGson = GsonUtil.getGsonBuilder(Map.of()).create();

    public static <V extends Codable> V fromProperties(Map<String, String> props, Class<? extends Codable> type) {
        String prefix = TYPE_TO_PREFIX.getOrDefault(type, "");
        Map<String, String> normalized = Util.removePrefix(prefix, props);
        normalized.keySet().stream().toList().stream()
                .filter(k -> StringUtils.countMatches(k, ".") > 0)
                .forEachOrdered(normalized::remove);
        return gsonUtil.rebuild(normalized, type);
    }

    public static <T extends Codable> Map<String, String> toProperties(T src) {
        String prefix = TYPE_TO_PREFIX.getOrDefault(src.getClass(), "");
        Map<String, String> defmap = getDefs(src);
//        Map<String, String> defmap = Collections.emptyMap();
        Map<String, String> flattened = flatten(src);
        return flattened.entrySet().stream()
                .filter(e -> !Objects.equals(e.getValue(), defmap.get(e.getKey())))
                .map(e -> Map.entry(prefix + e.getKey(), e.getValue()))
                .collect(CollectorsEx.toTreeMap());
    }

    private static <T extends Codable> Map<String, String> getDefs(T src) {
//        Class<? extends Codable> t = src.getClass();
        Class<? extends Codable> type = src.getClass();
        return defaultsCache.computeIfAbsent(type, c -> {
            Optional<T> def = Optional.ofNullable(GsonUtil.getTypeAdapters().get(type))
                    .filter(InstanceCreator.class::isInstance)
                    .map(InstanceCreator.class::cast)
                    .<T>map(creator -> (T)creator.createInstance(type));
//            T inst = c.acquireDefault();
//            return inst;
//            Object inst = gsonUtil.rebuild(new JsonObject(), c);
            Map<String, String> flattened = def.map(v -> flatten(v)).orElseGet(Collections::emptyMap);
            return flattened;
        });
    }

    private static Map<String, String> flatten(Object src) {
        JsonElement json = GsonUtil.flatten(rawGson.toJsonTree(src, src.getClass()));
        Map<String, String> map = rawGson.fromJson(json, STR_STR_MAP_TT.getType());
        return new TreeMap<>(map);
//        return gsonUtil.flatten(Objects.requireNonNull(src, "src is null"));
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

    @SuppressWarnings("unchecked")
    private static <T> T acquireDefault(Class<?> type) {
        return GitverConfig.acquireDefault(type);
//        try {
//            Object builder = type.getDeclaredMethod("builder").invoke(null);
//            Class<?> builderClass = builder.getClass();
//            Method buildMethod = builderClass.getMethod("build");
//            return (T)buildMethod.invoke(builder);
//        } catch (ReflectiveOperationException e) {
//            throw new GitverException(e.getMessage(), e);
//        }
    }

}
