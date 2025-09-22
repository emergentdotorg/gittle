package org.emergent.maven.gitver.core.version;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import org.emergent.maven.gitver.core.GitverConfig;

import java.util.Optional;

@Value
@NonFinal
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@lombok.experimental.FieldDefaults(level = AccessLevel.PROTECTED)
@lombok.experimental.Accessors(fluent = false)
public class ResolvedData extends GitverConfig {

//    @NonNull
    @lombok.Builder.Default
    String branch = "";
//    @NonNull
    @lombok.Builder.Default
    String hash = "";
//    @NonNull
    @lombok.Builder.Default
    String tagged = "0.0.0";
    @lombok.Builder.Default
    int commits = 0;
    @lombok.Builder.Default
    boolean dirty = false;

    public String getHashShort() {
        return Optional.ofNullable(hash).map(s -> s.substring(0, Math.min(8, s.length()))).orElse("");
    }

    public GitverConfig getConfig() {
        return GitverConfig.builder()
                .newVersion(getNewVersion())
                .releaseBranches(getReleaseBranches())
                .tagNamePattern(getTagNamePattern())
                .versionPattern(getVersionPattern())
                .build();
    }
}
