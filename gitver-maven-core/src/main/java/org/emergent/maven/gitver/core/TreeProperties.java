package org.emergent.maven.gitver.core;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

@Value
public class TreeProperties {

    @NonNull
    TreeMap<String, TreeNode> properties;

    public TreeProperties() {
        this(Collections.emptyMap());
    }

    public TreeProperties(Map<String, TreeNode> props) {
        this.properties = new TreeMap<>(props);
    }

    @Value
    @AllArgsConstructor(access = AccessLevel.PUBLIC)
    public static class TreeNode {

        public TreeNode() {
            this(JsonNull.INSTANCE);
        }

        @NonNull
        JsonElement value;
    }

    public static abstract class AbstractNodeGsonAdapter<T> implements
            JsonSerializer<T>,
            JsonDeserializer<T>
    {
        protected final Gson gson = getGson();

        private static Gson getGson() {
            return GsonUtil.getGsonBuilder(Map.of(
                    TreeNode.class, new TreeNodeGsonAdapter(),
                    TreeProperties.class, new TreePropertiesGsonAdapter()
            )).create();
        }
    }


    public static class TreeNodeGsonAdapter extends AbstractNodeGsonAdapter<TreeNode> {

        @Override
        public JsonElement serialize(TreeNode src, Type type, JsonSerializationContext ctx) {
            return src.value.deepCopy();
        }

        @Override
        public TreeNode deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
                throws JsonParseException {
            return new TreeNode(json.deepCopy());
        }
    }

    public static class TreePropertiesGsonAdapter extends AbstractNodeGsonAdapter<TreeProperties> {

        private static final TypeToken<TreeMap<String, TreeNode>> NODE_MAP_TT = new TypeToken<>() {
        };

        @Override
        public JsonElement serialize(TreeProperties src, Type type, JsonSerializationContext ctx) {
            return gson.toJsonTree(src.properties, NODE_MAP_TT.getType());
        }

        @Override
        public TreeProperties deserialize(JsonElement json, Type type, JsonDeserializationContext ctx)
                throws JsonParseException {
            return new TreeProperties(gson.fromJson(json, NODE_MAP_TT.getType()));
        }
    }

}
