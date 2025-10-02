package org.emergent.maven.gitver.core;

import org.emergent.maven.gitver.core.version.PatternStrategy;

public class Constants {
    public static final String GITTLE_PREFIX = "gittle.";

    public static final String TAG_PATTERN_DEF = "v?([0-9]+\\.[0-9]+\\.[0-9]+)";

    public static final String RELEASE_BRANCHES_DEF = "main,master";

    public static final String VERSION_PATTERN_DEF = PatternStrategy.VERSION_PATTERN_DEF;

    public static final String GITTLE = "gittle";
    public static final String NEW_VERSION = "newVersion";
    public static final String RELEASE_BRANCHES = "releaseBranches";
    public static final String TAG_NAME_PATTERN = "tagNamePattern";
    public static final String VERSION_PATTERN = "versionPattern";
}
