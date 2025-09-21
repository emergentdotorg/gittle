package org.emergent.maven.gitver.core.version;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.emergent.maven.gitver.core.CollectorsEx;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.PropCodec;
import org.emergent.maven.gitver.core.Util;

import static org.emergent.maven.gitver.core.Constants.GITTLE_PREFIX;
import static org.emergent.maven.gitver.core.Constants.GITTLE_RESOLVED_PREFIX;
import static org.emergent.maven.gitver.core.Util.startsWith;
import static org.emergent.maven.gitver.core.Util.substringAfter;

public record OverrideStrategy(String newVersion) implements VersionStrategy {

    public OverrideStrategy(GitverConfig config) {
        this(config.getNewVersion());
    }

    @Override
    public String toVersionString() {
        return newVersion;
    }

    @Override
    public GitverConfig getConfig() {
        return GitverConfig.builder().setNewVersion(newVersion).build();
    }

    public static OverrideStrategy from(Properties props) {
        return from(Util.toStringStringMap(props));
    }

    public static OverrideStrategy from(Map<String, String> props) {
        return new OverrideStrategy(GitverConfig.from(props));
    }

    @Override
    public Map<String, String> asMap() {
        return getConfig().asMap();
    }
}
