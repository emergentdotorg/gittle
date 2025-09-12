package org.emergent.maven.gitver.core;

public class Constants {
  public static final String PROPERTY_INBOUND_PREFIX = "gv.";
  public static final String PROPERTY_OUTBOUND_PREFIX = "gitver.";

  public static final String GV_DISABLED = "gv.disabled";
  public static final String GV_INITIAL_MAJOR = "gv.initial.major";
  public static final String GV_INITIAL_MINOR = "gv.initial.minor";
  public static final String GV_INITIAL_PATCH = "gv.initial.patch";
  public static final String GV_KEYWORDS_MAJOR = "gv.keywords.major";
  public static final String GV_KEYWORDS_MINOR = "gv.keywords.minor";
  public static final String GV_KEYWORDS_PATCH = "gv.keywords.patch";
  public static final String GV_KEYWORDS_REGEX = "gv.keywords.regex";
  public static final String GV_VERSION_PATTERN = "gv.version.pattern";
  public static final String GV_VERSION_OVERRIDE = "gv.version.override";

  public static final String GITVER_VERSION = "gitver.version";
  public static final String GITVER_BRANCH = "gitver.branch";
  public static final String GITVER_HASH = "gitver.hash";
  public static final String GITVER_HASH_SHORT = "gitver.hash.short";
  public static final String GITVER_MAJOR = "gitver.major";
  public static final String GITVER_MINOR = "gitver.minor";
  public static final String GITVER_PATCH = "gitver.patch";
  public static final String GITVER_COMMIT_NUMBER = "gitver.commitNumber";

  public static final String DEFAULT_MAJOR_KEYWORD = "[major]";
  public static final String DEFAULT_MINOR_KEYWORD = "[minor]";
  public static final String DEFAULT_PATCH_KEYWORD = "[patch]";
  public static final String DEFAULT_VERSION_PATTERN = "%M.%m.%p(-%c)(-%S)";
}
