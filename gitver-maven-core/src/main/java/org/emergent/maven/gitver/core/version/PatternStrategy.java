package org.emergent.maven.gitver.core.version;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.Mapper;

import static org.emergent.maven.gitver.core.Constants.GITVER_BRANCH;
import static org.emergent.maven.gitver.core.Constants.GITVER_COMMIT_NUMBER;
import static org.emergent.maven.gitver.core.Constants.GITVER_HASH;
import static org.emergent.maven.gitver.core.Constants.GITVER_HASH_SHORT;
import static org.emergent.maven.gitver.core.Constants.GITVER_MAJOR;
import static org.emergent.maven.gitver.core.Constants.GITVER_MINOR;
import static org.emergent.maven.gitver.core.Constants.GITVER_PATCH;
import static org.emergent.maven.gitver.core.Constants.GITVER_VERSION;

@Getter
@Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class PatternStrategy implements VersionStrategy {

  private final GitverConfig config;
  private final RefData refData;
  private final BasicVersion resolved;

  @Override
  public String toVersionString() {
    RefData version = getRefData();
    String pattern = getVersionPattern();

    int commits = resolved.getCommit();
    Map<PatternToken, Object> values = new HashMap<>();
    values.put(PatternToken.MAJOR, resolved.getMajor());
    values.put(PatternToken.MINOR, resolved.getMinor());
    values.put(PatternToken.PATCH, resolved.getPatch());
    values.put(PatternToken.SMART_PATCH, commits > 0 ? resolved.getPatch() + 1 : resolved.getPatch());
    values.put(PatternToken.COMMIT, commits);
    values.put(PatternToken.SNAPSHOT, commits > 0 ? "SNAPSHOT" : "");
    values.put(PatternToken.BRANCH, version.getBranch());
    values.put(PatternToken.HASH_SHORT, version.getHashShort());
    values.put(PatternToken.HASH, version.getHash());

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

  @Override
  public Map<String, String> toProperties() {
    Mapper m = Mapper.create()
      .putAll(config.toProperties())
      .put(GITVER_BRANCH, refData.getBranch())
      .put(GITVER_HASH, refData.getHash())
      .put(GITVER_HASH_SHORT, refData.getHashShort())
      .put(GITVER_MAJOR, resolved.getMajor(), -1)
      .put(GITVER_MINOR, resolved.getMinor(), -1)
      .put(GITVER_PATCH, resolved.getPatch(), -1)
      .put(GITVER_COMMIT_NUMBER, resolved.getCommit(), -1)
      .put(GITVER_VERSION, toVersionString());
    return m.toMap();
  }

  @Override
  public String toString() {
    return String.format("%s [branch: %s, version: %s, hash: %s]",
      getClass().getSimpleName(), refData.getBranch(), toVersionString(), refData.getHash());
  }

  private String getVersionPattern() {
    return config.getVersionPattern();
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
