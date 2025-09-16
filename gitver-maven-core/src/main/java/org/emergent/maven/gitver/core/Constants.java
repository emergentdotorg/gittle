package org.emergent.maven.gitver.core;

import org.emergent.maven.gitver.core.version.PatternStrategy;

public class Constants {
    public static final String PROPERTY_INBOUND_PREFIX = "gv.";
    public static final String PROPERTY_OUTBOUND_PREFIX = "gitver.";

    public static final String GV_TAG_PATTERN = "gv.tag.pattern";
    public static final String TAG_PATTERN_DEF = "v?([0-9]+\\.[0-9]+\\.[0-9]+)";

    public static final String GV_RELEASE_BRANCHES = "gv.version.initial";
    public static final String RELEASE_BRANCHES_DEF = "main,master";

    public static final String GV_VERSION_OVERRIDE = "gv.version.override";

    public static final String GV_VERSION_PATTERN = "gv.version.pattern";
    public static final String VERSION_PATTERN_DEF = PatternStrategy.VERSION_PATTERN_DEF;

    public static final String GITVER_VERSION = "gitver.version";
    public static final String GITVER_BRANCH = "gitver.branch";
    public static final String GITVER_HASH = "gitver.hash";
    public static final String GITVER_HASH_SHORT = "gitver.hash.short";
    public static final String GITVER_COMMIT_NUMBER = "gitver.commitNumber";
}
