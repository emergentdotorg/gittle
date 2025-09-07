package org.emergent.maven.gitver.core.version;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Getter;
import org.emergent.maven.gitver.core.VersionConfig;

@Getter
public class VersionPatternStrategy implements VersionStrategy {

  private final VersionConfig versionConfig;
  private final String branch;
  private final String hash;
  private int major;
  private int minor;
  private int patch;
  private int commit;

  public static VersionPatternStrategy create(String branch, String hash, VersionConfig versionConfig) {
    return new VersionPatternStrategy(branch, hash, versionConfig);
  }

  private VersionPatternStrategy(String branch, String hash, VersionConfig versionConfig) {
    this.versionConfig = versionConfig;
    this.branch = branch;
    this.hash = hash;
    this.major = versionConfig.getInitialMajor();
    this.minor = versionConfig.getInitialMinor();
    this.patch = versionConfig.getInitialPatch();
    this.commit = 0;
  }

  public RefVersionData getRefVersionData() {
    return RefVersionData.builder()
      .setBranch(branch)
      .setHash(hash)
      .setMajor(major)
      .setMinor(minor)
      .setPatch(patch)
      .setCommit(commit)
      .build();
  }

  public void resetVersion(int major, int minor, int patch) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.commit = 0;
  }

  public void incrementMajor() {
    major++;
    minor = 0;
    patch = 0;
    commit = 0;
  }

  public void incrementMinor() {
    minor++;
    patch = 0;
    commit = 0;
  }

  public void incrementPatch() {
    patch++;
    commit = 0;
  }

  public void incrementCommit() {
    commit++;
  }

  @Override
  public String toVersionString() {
    return toString(getRefVersionData(), getVersionPattern());
  }

  private String getVersionPattern() {
    return getVersionConfig().getVersionPattern();
  }

  @Override
  public String toString() {
    return String.format("%s [branch: %s, version: %s, hash: %s]",
      getClass().getSimpleName(), branch, toVersionString(), hash);
  }

  private static String toString(RefVersionData version, String pattern) {
    int commits = version.commit();
    Map<PatternToken, Object> values = new HashMap<>();
    values.put(PatternToken.MAJOR, version.major());
    values.put(PatternToken.MINOR, version.minor());
    values.put(PatternToken.PATCH, version.patch());
    values.put(PatternToken.SMART_PATCH, commits > 0 ? version.patch() + 1 : version.patch());
    values.put(PatternToken.COMMIT, commits);
    values.put(PatternToken.SNAPSHOT, commits > 0 ? "SNAPSHOT" : "");
    values.put(PatternToken.BRANCH, version.branch());
    values.put(PatternToken.HASH_SHORT, version.getHashShort());
    values.put(PatternToken.HASH, version.hash());

    String text = pattern;
    for (PatternToken token : values.keySet()) {
      String value = Optional.ofNullable(values.get(token)).map(java.lang.String::valueOf).orElse("");
      boolean emptyValue = value.isEmpty() || "0".equals(value);

      // An empty or '0' value means we remove the whole group.
      String replacement = emptyValue ? "" : "$1" + value + "$2";
      String regex = "\\(([^(]*)" + token.getToken() + "([^)]*)\\)";
      text = text.replaceAll(regex, replacement);

      // Now we replace the token if it wasn't in a group
      text = text.replace(token.getToken(), value);
    }
    return text;
  }

  @Getter
  public enum PatternToken {
    MAJOR("M"),
    MINOR("m"),
    PATCH("p"),
    SMART_PATCH("P"),
    COMMIT("c"),
    SNAPSHOT("S"),
    BRANCH("b"),
    HASH_SHORT("h"),
    HASH("H");

    private final String token;

    PatternToken(String token) {
      this.token = "%" + token;
    }

    @Override
    public String toString() {
      return getToken();
    }
  }
}
