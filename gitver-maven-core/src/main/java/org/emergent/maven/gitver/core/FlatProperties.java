package org.emergent.maven.gitver.core;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import lombok.NonNull;
import lombok.Value;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Value
public class FlatProperties {

    @NonNull
    LinkedHashMap<String, String> properties;

    public FlatProperties() {
        this(Collections.emptyMap());
    }

    public FlatProperties(Map<String, String> props) {
        this.properties = new LinkedHashMap<>(props);
    }

    public static class GsonAdapter implements
//            InstanceCreator<PatternStrategy>,
            JsonSerializer<FlatProperties>,
            JsonDeserializer<FlatProperties>
    {
        private static final Map<String, String> NAME_REPL_MAP = Map.of("config", "gittle");
        private static final Map<String, String> REPL_NAME_MAP = Util.getReversed(NAME_REPL_MAP);
        private static final String STANDARD_PREFIX = "gittle_resolved_";
        private static final String VERSION_STRING = "versionString";

        private final Gson gson = GsonUtil.getGsonBuilder(Map.of(FlatProperties.class, new GsonAdapter())).create();

//        @Override
//        public PatternStrategy createInstance(Type type) {
//            return PatternStrategy.create();
//        }

        @Override
        public JsonElement serialize(FlatProperties src, Type type, JsonSerializationContext ctx) {
            JsonElement flatEl = gson.toJsonTree(src.properties, GsonUtil.STR_STR_MAP_TT.getType());
            return GsonUtil.rebuild(flatEl);
        }

        @Override
        public FlatProperties deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
                throws JsonParseException {
            JsonElement flatEl = GsonUtil.flatten(json);
            return new FlatProperties(gson.fromJson(flatEl, GsonUtil.STR_STR_MAP_TT.getType()));
        }
    }
}
