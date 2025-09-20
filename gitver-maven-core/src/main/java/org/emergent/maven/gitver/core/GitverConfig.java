package org.emergent.maven.gitver.core;

import static org.emergent.maven.gitver.core.Constants.RELEASE_BRANCHES_DEF;
import static org.emergent.maven.gitver.core.Constants.TAG_PATTERN_DEF;
import static org.emergent.maven.gitver.core.Constants.VERSION_PATTERN_DEF;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
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

    public static final GitverConfig DEFAULT = GitverConfig.builder().build();

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

//    public Set<String> getReleaseBranchesSet() {
//        return Arrays.stream(Optional.ofNullable(getReleaseBranches())
//                        .orElse(RELEASE_BRANCHES_DEF)
//                        .split(","))
//                .map(String::trim)
//                .collect(Collectors.toCollection(TreeSet::new));
//    }

    public Set<String> getReleaseBranchesSet() {
        return Arrays.stream(releaseBranches.split(","))
          .map(String::trim)
          .collect(Collectors.toCollection(TreeSet::new));
    }

    public static GitverConfig from(Properties props) {
        return PropCodec.getInstance().fromProperties(props, GitverConfig.class);
    }

    public Properties toProperties() {
        Properties props = new Properties();
        props.putAll(PropCodec.getInstance().toProperties(this, DEFAULT, this.getClass()));
        return props;
    }

    public Map<String, String> asMap() {
        return PropCodec.getInstance().toProperties(this, DEFAULT, this.getClass());
    }
}
