package org.emergent.maven.gitver.core.version;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import lombok.Builder;
import lombok.Getter;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.VersionConfig;

@Getter
@Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class PatternStrategy implements VersionStrategy {

  private final VersionConfig versionConfig;
  private final String branch;
  private final String hash;
  private int major;
  private int minor;
  private int patch;
  private int commit;

  private RefVersionData getRefVersionData() {
    return RefVersionData.builder()
      .setBranch(branch)
      .setHash(hash)
      .setMajor(major)
      .setMinor(minor)
      .setPatch(patch)
      .setCommit(commit)
      .build();
  }

  @Override
  public String toVersionString() {
    return toString(getRefVersionData(), getVersionPattern());
  }

  private String getVersionPattern() {
    return versionConfig.getVersionPattern();
  }

  public Map<String, String> toProperties() {
    Map<String, Object> properties = new TreeMap<>();
    properties.put(VersionConfig.GITVER_VERSION, toVersionString());
    properties.putAll(getRefVersionData().toProperties());
    properties.putAll(versionConfig.toProperties());
    return Util.flatten(properties);
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

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder {

    public Builder incrementMajor() {
      return this
        .setMajor(major + 1)
        .setMinor(0)
        .setPatch(0)
        .setCommit(0);
    }

    public Builder incrementMinor() {
      return this
        .setMinor(minor + 1)
        .setPatch(0)
        .setCommit(0);
    }

    public Builder incrementPatch() {
      return this
        .setPatch(patch + 1)
        .setCommit(0);
    }

    public Builder incrementCommit() {
      return setCommit(commit + 1);
    }
  }
}
