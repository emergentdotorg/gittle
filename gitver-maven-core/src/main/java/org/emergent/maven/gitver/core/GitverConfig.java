package org.emergent.maven.gitver.core;

import static org.emergent.maven.gitver.core.Constants.RELEASE_BRANCHES_DEF;
import static org.emergent.maven.gitver.core.Constants.TAG_PATTERN_DEF;
import static org.emergent.maven.gitver.core.Constants.VERSION_PATTERN_DEF;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Value
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@lombok.Builder(toBuilder = true, setterPrefix = "set", builderClassName = "Builder")
public class GitverConfig {

    @NonNull
    @lombok.Builder.Default
    String tagPattern = TAG_PATTERN_DEF;

    @NonNull
    @lombok.Builder.Default
    String versionOverride = "";

    @NonNull
    @lombok.Builder.Default
    String versionPattern = VERSION_PATTERN_DEF;

    @NonNull
    @lombok.Builder.Default
    String releaseBranches = RELEASE_BRANCHES_DEF;

    public static GitverConfig from(Properties props) {
        return PropertiesCodec.fromMap(Util.flatten(props));
        // Builder builder = builder();
        // return builder
        //         .setTagPattern(props.getProperty(GV_TAG_PATTERN, TAG_PATTERN_DEF))
        //         .setVersionOverride(props.getProperty(GV_VERSION_OVERRIDE, ""))
        //         .setVersionPattern(props.getProperty(GV_VERSION_PATTERN, VERSION_PATTERN_DEF))
        //         .setReleaseBranches(props.getProperty(GV_RELEASE_BRANCHES, RELEASE_BRANCHES_DEF))
        //         .build();
    }

    public Set<String> getReleaseBranchesSet() {
        return Arrays.stream(releaseBranches.split(","))
                .map(String::trim)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public Properties toProperties() {
        Mapper mapper = Mapper.create();
        PropertiesCodec.toMap(this).forEach((k,v) -> mapper.put(k, String.valueOf(v)));
        return mapper
                // .put(GV_TAG_PATTERN, getTagPattern(), TAG_PATTERN_DEF)
                // .put(GV_VERSION_OVERRIDE, getVersionOverride(), "")
                // .put(GV_VERSION_PATTERN, getVersionPattern(), VERSION_PATTERN_DEF)
                // .put(GV_RELEASE_BRANCHES, getReleaseBranches(), RELEASE_BRANCHES_DEF)
                .toProperties();
    }

    public static class PropertiesCodec {

        private static final TypeToken<Map<String, Object>> MAP_TYPE_TOKEN = new TypeToken<>() {};
        private static final TypeToken<GitverConfig> CONFIG_BASE_TYPE_TOKEN = new TypeToken<>() {};
        private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        public static Map<String, Object> toMap(GitverConfig bean) {
            JsonElement json = gson.toJsonTree(bean, CONFIG_BASE_TYPE_TOKEN.getType());
            Map<String, Object> outMap = gson.fromJson(json, MAP_TYPE_TOKEN.getType());
            System.out.printf("GitverConfig properties: %s%n", outMap.entrySet().stream()
              .map(e -> e.getKey() + "=" + e.getValue()
              ).collect(Collectors.joining("\n", "\n", "\n")));
            return outMap;
        }

        public static GitverConfig fromMap(Properties map) {
            return fromMap(Util.flatten(map));
            // JsonElement json = gson.toJsonTree(map, MAP_TYPE_TOKEN.getType());
            // return gson.fromJson(json, CONFIG_BASE_TYPE_TOKEN.getType());
        }

        public static GitverConfig fromMap(Map<String, ?> map) {
            JsonElement json = gson.toJsonTree(map, MAP_TYPE_TOKEN.getType());
            return gson.fromJson(json, CONFIG_BASE_TYPE_TOKEN.getType());
        }
    }


}
