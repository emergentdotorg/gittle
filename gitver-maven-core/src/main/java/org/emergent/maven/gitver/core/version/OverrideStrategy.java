package org.emergent.maven.gitver.core.version;

import java.util.Map;
import java.util.Properties;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.PropCodec;

public record OverrideStrategy(String versionOverride) implements VersionStrategy<OverrideStrategy> {

    private static final OverrideStrategy DEFAULT = new OverrideStrategy(GitverConfig.DEFAULT);

    public OverrideStrategy(GitverConfig config) {
        this(config.getVersionOverride());
    }

    @Override
    public String toVersionString() {
        return versionOverride;
    }

    @Override
    public GitverConfig getConfig() {
        return GitverConfig.builder().setVersionOverride(versionOverride).build();
    }

    public static OverrideStrategy from(Properties props) {
        return PropCodec.getInstance().fromProperties(props, OverrideStrategy.class);
    }

    @Override
    public Properties toProperties() {
        Properties props = new Properties();
        props.putAll(PropCodec.getInstance().toProperties(this, DEFAULT, GitverConfig.class));
        return props;
    }

    @Override
    public Map<String, String> asMap() {
        return PropCodec.getInstance().toProperties(this, DEFAULT, this.getClass());
    }
}
