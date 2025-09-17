package org.emergent.maven.gitver.core;

import static org.emergent.maven.gitver.core.Constants.RELEASE_BRANCHES_DEF;
import static org.emergent.maven.gitver.core.Constants.TAG_PATTERN_DEF;
import static org.emergent.maven.gitver.core.Constants.VERSION_PATTERN_DEF;
import static org.emergent.maven.gitver.core.version.PropertiesCodec.toMap;

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
import org.emergent.maven.gitver.core.version.PropertiesCodec;

@Value
@Slf4j
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@lombok.Builder(toBuilder = true, setterPrefix = "set", builderClassName = "Builder")
public class GitverConfig {

    private static final Map<String, Object> DEFAULTS =
            PropertiesCodec.toMap(GitverConfig.builder().build());

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
        return org.emergent.maven.gitver.core.version.PropertiesCodec.toGitverConfig(Util.flatten(props));
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
        MapperEx mapper = MapperEx.create(DEFAULTS);
        mapper.putAll(toMap(this));
        return mapper.toProperties();
    }
}
