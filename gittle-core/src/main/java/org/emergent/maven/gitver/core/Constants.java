package org.emergent.maven.gitver.core;

import org.emergent.maven.gitver.core.version.PatternStrategy;

public class Constants {
    public static final String PROPERTY_INBOUND_PREFIX = "gittle.";
    public static final String PROPERTY_OUTBOUND_PREFIX = "gitver.";

    public static final String GITTLE_RESOLVED_PREFIX = "gittle.resolved.";
    public static final String GITTLE_PREFIX = "gittle.";

    public static final String GV_TAG_PATTERN = "gittle.tagNamePattern";
    public static final String TAG_PATTERN_DEF = "v?([0-9]+\\.[0-9]+\\.[0-9]+)";

    public static final String GV_RELEASE_BRANCHES = "gittle.version.initial";
    public static final String RELEASE_BRANCHES_DEF = "main,master";

    public static final String GV_VERSION_OVERRIDE = "gittle.newVersion";

    public static final String GV_VERSION_PATTERN = "gittle.versionPattern";
    public static final String VERSION_PATTERN_DEF = PatternStrategy.VERSION_PATTERN_DEF;

    public static final String GITVER_VERSION = "gitver.version";
    public static final String GITVER_BRANCH = "gitver.branch";
    public static final String GITVER_HASH = "gitver.hash";
    public static final String GITVER_HASH_SHORT = "gitver.hash.short";
    public static final String GITVER_COMMIT_NUMBER = "gitver.commitNumber";
    public static final String GITTLE = "gittle";
    public static final String NEW_VERSION = "newVersion";
    public static final String RELEASE_BRANCHES = "releaseBranches";
    public static final String TAG_NAME_PATTERN = "tagNamePattern";
    public static final String VERSION_PATTERN = "versionPattern";
}
