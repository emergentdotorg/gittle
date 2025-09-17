package org.emergent.maven.gitver.core.version;

import static org.emergent.maven.gitver.core.Constants.GITVER_VERSION;

import java.util.Map;
import java.util.Objects;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.MapperEx;

public record OverrideStrategy(GitverConfig config) implements VersionStrategy {

    public OverrideStrategy(String version) {
        this(GitverConfig.builder().setVersionOverride(version).build());
    }

    public OverrideStrategy(GitverConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    @Override
    public String toVersionString() {
        return config.getVersionOverride();
    }

    @Override
    public Map<String, String> getPropertiesMap() {
        MapperEx m = MapperEx.create().putAll(config.toProperties()).put(GITVER_VERSION, toVersionString());
        return m.toFlattened();
    }
}
