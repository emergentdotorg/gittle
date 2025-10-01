package org.emergent.maven.gitver.core.version;

import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.PropCodec;
import org.emergent.maven.gitver.core.Util;

import java.util.Map;
import java.util.Optional;

@Value
@NonFinal
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
public class OverrideStrategy extends GitverConfig implements VersionStrategy {

    public static OverrideStrategy from(String newVersion) {
        return builder().newVersion(newVersion).build();
    }

    static Optional<VersionStrategy> getOverrideStrategy(GitverConfig config) {
        return Optional.ofNullable(config)
                .filter(c -> Util.isNotEmpty(c.getNewVersion()))
                .map(conf -> builder()
                        .newVersion(conf.getNewVersion())
                        .build())
                .map(s -> s);
    }

    @Override
    public String toVersionString() {
        return getNewVersion();
    }

    @Override
    public GitverConfig getConfig() {
        return GitverConfig.builder()
                .newVersion(getNewVersion())
                .releaseBranches(getReleaseBranches())
                .tagNamePattern(getTagNamePattern())
                .versionPattern(getVersionPattern())
                .build();
    }

    @Override
    public Map<String, String> asMap() {
        return PropCodec.toProperties(this).getProperties();
    }
}
