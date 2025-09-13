package org.emergent.maven.gitver.core.version;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.Mapper;

import static org.emergent.maven.gitver.core.Constants.GITVER_VERSION;

@Getter
@Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class PatternStrategy implements VersionStrategy {

  private final GitverConfig config;
  private final RefData refData;

  private String getVersionPattern() {
    return config.getVersionPattern();
  }

  public Map<String, String> toProperties() {
    Mapper m = Mapper.create()
      .put(GITVER_VERSION, toVersionString())
      .putAll(getRefData().toProperties())
      .putAll(config.toProperties());
    return m.toMap();
  }

  @Override
  public String toString() {
    return String.format("%s [branch: %s, version: %s, hash: %s]",
      getClass().getSimpleName(), refData.getBranch(), toVersionString(), refData.getHash());
  }

  @Override
  public String toVersionString() {
    RefData version = getRefData();
    String pattern = getVersionPattern();

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
