package org.emergent.maven.gitver.core.version;

import static org.emergent.maven.gitver.core.Constants.GITVER_BRANCH;
import static org.emergent.maven.gitver.core.Constants.GITVER_COMMIT_NUMBER;
import static org.emergent.maven.gitver.core.Constants.GITVER_HASH;
import static org.emergent.maven.gitver.core.Constants.GITVER_HASH_SHORT;
import static org.emergent.maven.gitver.core.Constants.GITVER_VERSION;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.Mapper;

@Value
@Accessors(fluent = true)
@lombok.Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class PatternStrategy implements VersionStrategy {

    public static final String VERSION_PATTERN_DEF = "%t(-%B)(-%c)(-%S)+%h(.%d)";

    @NonNull
    GitverConfig config;

    @NonNull
    RefData ref;

    @lombok.Builder.Default
    @NonNull
    String tag = "0.0.0";

    int commits;
    boolean dirty;

    @Override
    public String toVersionString() {
        String pattern = config.getVersionPattern();
        Set<String> releaseBranches = config.getReleaseBranchesSet();
        String branch = ref.branch();

        Map<PatternToken, Object> values = new HashMap<>();
        values.put(PatternToken.TAG, tag);
        values.put(PatternToken.COMMIT, commits);
        values.put(PatternToken.SNAPSHOT, commits > 0 ? "SNAPSHOT" : "");
        values.put(PatternToken.BRANCH, branch);
        values.put(PatternToken.DEV_BRANCH, releaseBranches.contains(branch) ? "" : branch);
        values.put(PatternToken.HASH_SHORT, ref.getHashShort());
        values.put(PatternToken.HASH, ref.hash());
        values.put(PatternToken.DIRTY, dirty ? "dirty" : "");
        return performTokenReplacements(pattern, values);
    }

    private static String performTokenReplacements(String pattern, Map<PatternToken, Object> tokenValueMap) {
        String text = pattern;
        for (PatternToken token : tokenValueMap.keySet()) {
            String value = Optional.ofNullable(tokenValueMap.get(token))
                    .map(String::valueOf)
                    .orElse("");
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
    public Map<String, String> getPropertiesMap() {
        return Mapper.create()
                .putAll(config.toProperties())
                .put(GITVER_BRANCH, ref.branch(), "")
                .put(GITVER_HASH, ref.hash())
                .put(GITVER_HASH_SHORT, ref.getHashShort())
                .put(GITVER_COMMIT_NUMBER, commits, -1)
                .put(GITVER_VERSION, toVersionString())
                .toMap();
    }

    @Override
    public String toString() {
        return String.format(
                "%s [branch: %s, version: %s, hash: %s]",
                getClass().getSimpleName(), ref.branch(), toVersionString(), ref.hash());
    }

    @Getter
    public enum PatternToken {
        TAG("t"),
        COMMIT("c"),
        SNAPSHOT("S"),
        BRANCH("b"),
        DEV_BRANCH("B"),
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
