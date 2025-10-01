package org.emergent.maven.gitver.core.version;

import org.emergent.maven.gitver.core.GitverConfig;

import java.io.File;

public class StrategyFactory {

    public static VersionStrategy getVersionStrategy(File basePath, GitverConfig config) {
        return OverrideStrategy.getOverrideStrategy(config)
                .orElseGet(() -> PatternStrategy.getPatternStrategy(config, basePath));
    }
}
