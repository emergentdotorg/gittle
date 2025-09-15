package org.emergent.maven.gitver.core.version;

import static org.emergent.maven.gitver.core.Constants.GITVER_BRANCH;
import static org.emergent.maven.gitver.core.Constants.GITVER_COMMIT_NUMBER;
import static org.emergent.maven.gitver.core.Constants.GITVER_HASH;
import static org.emergent.maven.gitver.core.Constants.GITVER_HASH_SHORT;
import static org.emergent.maven.gitver.core.Constants.GITVER_VERSION;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.Mapper;
import org.emergent.maven.gitver.core.Util;

@Value
@Accessors(fluent = true)
@Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class PatternStrategy implements VersionStrategy {

    public static final String VERSION_PATTERN_DEF = "%t(-%B)(-%c)(-%S)+%h(.%d)";

    @NonNull GitverConfig config;
    @NonNull RefData ref;
    @NonNull String tag;
    int commits;
    boolean dirty;

    @Override
    public String toVersionString() {
        RefData version = ref;
        String pattern = config.getVersionPattern();
        Set<String> releaseBranches = Arrays.stream(config.getReleaseBranches().split(","))
          .collect(Collectors.toSet());
        String branch = version.getBranch();
        String develBranch = releaseBranches.contains(branch) ? "" : branch;

        Map<PatternToken, Object> values = new HashMap<>();
        values.put(PatternToken.TAG, tag);
        values.put(PatternToken.COMMIT, commits);
        values.put(PatternToken.SNAPSHOT, commits > 0 ? "SNAPSHOT" : "");
        values.put(PatternToken.BRANCH, branch);
        values.put(PatternToken.NON_RELEASE_BRANCH, develBranch);
        values.put(PatternToken.HASH_SHORT, version.getHashShort());
        values.put(PatternToken.HASH, version.getHash());
        values.put(PatternToken.DIRTY, dirty ? "dirty" : "");

        String text = pattern;
        for (PatternToken token : values.keySet()) {
            String value = Optional.ofNullable(values.get(token)).map(java.lang.String::valueOf).orElse("");
            boolean emptyValue = value.isEmpty() || "0".equals(value);

            // An empty or '0' value means we remove the whole group.
            String replacement = emptyValue ? "" : "$1" + value + "$2";

            String tokenPattern = Pattern.quote(token.token());

            String regex = "\\(([^(]*)" + tokenPattern + "([^)]*)\\)";
            text = text.replaceAll(regex, replacement);

            // Now we replace the token if it wasn't in a group
            text = text.replaceAll(tokenPattern, value);
        }
        return text;
    }

    @Override
    public Map<String, String> toProperties() {
        Mapper m = Mapper.create()
          .putAll(config.toProperties())
          .put(GITVER_BRANCH, ref.getBranch(), "")
          .put(GITVER_HASH, ref.getHash())
          .put(GITVER_HASH_SHORT, ref.getHashShort())
          .put(GITVER_COMMIT_NUMBER, commits, -1)
          .put(GITVER_VERSION, toVersionString());
        return m.toMap();
    }

    @Override
    public String toString() {
        return String.format(
          "%s [branch: %s, version: %s, hash: %s]",
          getClass().getSimpleName(), ref.getBranch(), toVersionString(), ref.getHash());
    }

    public static class Builder {

        public Builder setConfig(GitverConfig config) {
            if (Util.isEmpty(this.tag)) {
                setTag(config.getVersionInitial());
            }
            this.config = config;
            return this;
        }
    }

    @Getter
    public enum PatternToken {
        TAG("t"),
        COMMIT("c"),
        SNAPSHOT("S"),
        BRANCH("b"),
        NON_RELEASE_BRANCH("B"),
        HASH_SHORT("h"),
        HASH("H"),
        DIRTY("d");

        private final String token;

        PatternToken(String token) {
            this.token = "%" + Objects.requireNonNull(token);
        }

        @Override
        public String toString() {
            return token;
        }
    }
}
