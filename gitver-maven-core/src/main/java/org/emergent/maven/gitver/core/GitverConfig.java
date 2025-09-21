package org.emergent.maven.gitver.core;

import com.google.gson.annotations.SerializedName;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.emergent.maven.gitver.core.Constants.GITTLE_PREFIX;
import static org.emergent.maven.gitver.core.Constants.RELEASE_BRANCHES_DEF;
import static org.emergent.maven.gitver.core.Constants.TAG_PATTERN_DEF;
import static org.emergent.maven.gitver.core.Constants.VERSION_PATTERN_DEF;

@Slf4j
@Value
//@Accessors(fluent = true)
//@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
//@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@lombok.Builder(toBuilder = true, setterPrefix = "set", builderClassName = "Builder")
public class GitverConfig implements PropCodec.Codable<GitverConfig> {

    @SerializedName(Constants.NEW_VERSION)
    @NonNull
    @lombok.Builder.Default
    private String newVersion = "";

    @SerializedName(Constants.RELEASE_BRANCHES)
    @NonNull
    @lombok.Builder.Default
    private String releaseBranches = RELEASE_BRANCHES_DEF;

    @SerializedName(Constants.TAG_NAME_PATTERN)
    @NonNull
    @lombok.Builder.Default
    private String tagNamePattern = TAG_PATTERN_DEF;

    @SerializedName(Constants.VERSION_PATTERN)
    @NonNull
    @lombok.Builder.Default
    private String versionPattern = VERSION_PATTERN_DEF;

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

    public static GitverConfig from(Map<String, String> props) {
        return PropCodec.fromProperties(props, GitverConfig.class);
    }

    public Map<String, String> asMap() {
        return PropCodec.toProperties(this);
    }

    public Properties toProperties() {
        return Util.toProperties(asMap());
    }

    public static class Builder {

    }
}
