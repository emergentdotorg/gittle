package org.emergent.gittle.maven.plugin;

public class Constants {
    public static final String GITTLE = "gittle";
    public static final String GITTLE_PREFIX = GITTLE + ".";
    public static final String NEW_VERSION = "newVersion";
    public static final String RELEASE_BRANCHES = "releaseBranchRegex";
    public static final String TAG_NAME_PATTERN = "tagNameRegex";
    public static final String VERSION_PATTERN = "versionPattern";

    public static final String RELEASE_BRANCH_REGEX_DEF = "^(main|master)$";
    public static final String TAG_NAME_REGEX_DEF = "v?([0-9]+\\.[0-9]+\\.[0-9]+)";
    public static final String VERSION_PATTERN_DEF = "%t(-%B)(-%C)(-%S)(+%H)(.%D)";
}
