package org.emergent.maven.gitver.core;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import org.eclipse.jgit.api.Git;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.emergent.maven.gitver.core.Constants.RELEASE_BRANCHES_DEF;
import static org.emergent.maven.gitver.core.Constants.TAG_PATTERN_DEF;
import static org.emergent.maven.gitver.core.Constants.VERSION_PATTERN_DEF;

@Value
@NonFinal
@NoArgsConstructor
@AllArgsConstructor
@lombok.experimental.FieldDefaults(level = AccessLevel.PROTECTED)
@lombok.experimental.Accessors(fluent = false)
@SuperBuilder(toBuilder = true)
public class GitverConfig {

    @lombok.Builder.Default
    String basePath = ".";

    @lombok.Builder.Default
    String newVersion = "";

    @lombok.Builder.Default
    String releaseBranches = RELEASE_BRANCHES_DEF;

    @lombok.Builder.Default
    String tagNamePattern = TAG_PATTERN_DEF;

    @lombok.Builder.Default
    String versionPattern = VERSION_PATTERN_DEF;

    public Map<String, String> asMap() {
        return PropCodec.toProperties(this).getProperties();
    }

    public Set<String> getReleaseBranchesSet() {
        String branchesString = Optional.ofNullable(releaseBranches).orElse(RELEASE_BRANCHES_DEF);
        return Arrays.stream(branchesString.split(","))
                .map(String::trim).collect(Collectors.toCollection(TreeSet::new));
    }
}
