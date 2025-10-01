package org.emergent.maven.gitver.core;

import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import org.emergent.maven.gitver.core.version.OverrideStrategy;
import org.emergent.maven.gitver.core.version.PatternStrategy;
import org.emergent.maven.gitver.core.version.ResolvedData;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Value
@AllArgsConstructor
@Builder(toBuilder = true, setterPrefix = "set", builderClassName = "Builder")
public class GsonUtil {

    private static final Map<Boolean, GsonUtil> INSTANCES = new ConcurrentHashMap<>();

    private static final TypeToken<List<Object>> LIST_TT = new TypeToken<>() {
    };
    private static final TypeToken<Map<Object, Object>> OBJ_OBJ_MAP_TT = new TypeToken<>() {
    };
    public static final TypeToken<Map<String, String>> STR_STR_MAP_TT = new TypeToken<>() {
    };
    public static final TypeToken<Map<String, Object>> STR_OBJ_MAP_TT = new TypeToken<>() {
    };
    private static final Predicate<String> IS_INT = Pattern.compile("^[0-9]+$").asMatchPredicate();

    public static GsonBuilder getGsonBuilder(Map<Class<?>, Object> typeAdapters) {
        GsonBuilder builder = new GsonBuilder()
//                .setFieldNamingStrategy(new MyFieldNamingStrategy())
                .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
                .setNumberToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE);
        typeAdapters.forEach(builder::registerTypeAdapter);
        return builder;
    }
//
//    @Getter
//    boolean pretty;
//    Gson gson;
//
//    public static GsonUtil getInstance() {
//        return getInstance(false);
//    }
//
//    public static GsonUtil getInstance(boolean pretty) {
//        return INSTANCES.computeIfAbsent(pretty, GsonUtil::new);
//    }
//
//    private GsonUtil(boolean pretty) {
//        this.pretty = pretty;
//        GsonBuilder builder = Util.getGsonBuilder(getTypeAdapters());
//        if (pretty) {
//            builder.setPrettyPrinting();
//        }
//        this.gson = builder.create();
//    }
//
//    public static Map<Class<?>, Object> getTypeAdapters() {
//        return Map.of(
////                GitverConfig.class, new GitverConfigGsonAdapter(),
////                ResolvedData.class, new ResolvedDataGsonAdapter(),
////                PatternStrategy.class, new PatternStrategyGsonAdapter()
//        );
//    }
//
//    static GsonBuilder getGsonBuilder() {
//        return Util.getGsonBuilder(Map.of());
//    }
//
//    public Map<String, Object> toMapTree(Object src) {
//        Map<String, String> flattened = flatten(src);
//        JsonElement tree = gson.toJsonTree(flattened, STR_STR_MAP_TT.getType());
//        return gson.fromJson(tree, STR_OBJ_MAP_TT.getType());
//    }
//
//    public Map<String, String> flatten(Object src) {
//        if (src instanceof Map<?, ?> map) {
//            return flattenMap(map);
//        } else {
//            JsonElement json = gson.toJsonTree(src, src.getClass());
//            Map<String, String> map = gson.fromJson(json, STR_STR_MAP_TT.getType());
//            return new TreeMap<>(map);
//        }
//    }
//
//    public Map<String, String> flattenMap(Map<?, ?> src) {
//        Map<String, Object> map = src.entrySet().stream()
//                .filter(e -> e.getKey() instanceof String)
//                .collect(CollectorsEx.toLinkedHashMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue));
//        JsonElement json = TreeUtil.flatten(gson.toJsonTree(map, STR_OBJ_MAP_TT.getType()));
//        return gson.fromJson(json, STR_STR_MAP_TT.getType());
//    }
//
//
//
//    public <V> V rebuild(Map<String, ?> in, Type type) {
//        JsonElement json = gson.toJsonTree(in, STR_OBJ_MAP_TT.getType());
//        return rebuild(json, type);
//    }
//
//    public <V> V rebuild(JsonElement json, Type type) {
//        JsonElement rebuilt = TreeUtil.rebuild(json);
//        return gson.fromJson(rebuilt, type);
//    }

    private static class GitverConfigGsonAdapter implements
            InstanceCreator<GitverConfig>
    {
        @Override
        public GitverConfig createInstance(Type type) {
            return GitverConfig.builder().build();
        }
    }

    private static class ResolvedDataGsonAdapter implements
            InstanceCreator<ResolvedData>
    {
        @Override
        public ResolvedData createInstance(Type type) {
            return ResolvedData.builder().build();
        }
    }

    public static class PatternStrategyGsonAdapter implements
            InstanceCreator<PatternStrategy>,
            JsonSerializer<PatternStrategy>,
            JsonDeserializer<PatternStrategy>
    {
        private final Gson gson = getGsonBuilder(Map.of()).create();

        @Override
        public PatternStrategy createInstance(Type type) {
            return PatternStrategy.builder().build();
        }

        @Override
        public JsonElement serialize(PatternStrategy src, Type type, JsonSerializationContext ctx) {
            return gson.toJsonTree(src, type);
        }

        @Override
        public PatternStrategy deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
                throws JsonParseException {
            return gson.fromJson(json, type);
        }
    }

    public static class PropertiesGsonAdapter implements JsonSerializer<Properties>, JsonDeserializer<Properties> {

        public static final PropertiesGsonAdapter INSTANCE = new PropertiesGsonAdapter();

        private final Gson gson;

        public PropertiesGsonAdapter() {
            this.gson = getGsonBuilder(Map.of()).create();
        }

        @Override
        public Properties deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
                throws JsonParseException {
            Map<String, String> map = ctx.deserialize(json, STR_STR_MAP_TT.getType());
            Properties props = new Properties();
            props.putAll(map);
            return props;
        }

        @Override
        public JsonElement serialize(Properties src, Type type, JsonSerializationContext ctx) {
            return gson.toJsonTree(src, STR_STR_MAP_TT.getType());
        }
    }

    public static class MyFieldNamingStrategy implements FieldNamingStrategy {

        private static final String GITTLE_RESOLVED_ = "gittle_resolved_";
        private static final String GITTLE_ = "gittle_";
        public static final Map<Class<?>, String> PREFIX_MAP = Map.of(
                OverrideStrategy.class, GITTLE_RESOLVED_,
                PatternStrategy.class, GITTLE_RESOLVED_,
                ResolvedData.class, GITTLE_RESOLVED_,
                GitverConfig.class, GITTLE_
        );

        @Override
        public String translateName(Field f) {
            String name = f.getName();
            Optional<String> prefix = Optional.ofNullable(PREFIX_MAP.get(f.getDeclaringClass()));
            return prefix.map(p -> p + name).orElse(name);
        }

        @Override
        public List<String> alternateNames(Field f) {
            Optional<String> prefix = Optional.ofNullable(PREFIX_MAP.get(f.getDeclaringClass()));
            return prefix.map(p -> Collections.singletonList(p + f.getName()))
                    .orElseGet(Collections::emptyList);
        }
    }
}
