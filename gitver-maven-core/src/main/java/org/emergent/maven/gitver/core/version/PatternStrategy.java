package org.emergent.maven.gitver.core.version;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.PropCodec;
import org.emergent.maven.gitver.core.Util;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.regex.Pattern.quote;
import static org.emergent.maven.gitver.core.Constants.GITTLE_PREFIX;
import static org.emergent.maven.gitver.core.Constants.GITTLE_RESOLVED_PREFIX;

@Value
//@Accessors(fluent = true)
//@NoArgsConstructor(access = AccessLevel.PUBLIC)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@lombok.Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class PatternStrategy implements VersionStrategy {
    public static final String VERSION_PATTERN_DEF = "%t(-%B)(-%c)(-%S)+%h(.%d)";

    private static final String STANDARD_PREFIX = "gittle.resolved.";
    private static final String VERSION_STRING = "version";
    private static final String RESOLVED_PREFIX = "resolved.";

    @NonNull
    @lombok.Builder.Default
    private GitverConfig config = GitverConfig.builder().build();

    @NonNull
    @lombok.Builder.Default
    private ResolvedData resolved = ResolvedData.builder().build();

    // @NonNull
    // @lombok.Builder.Default
    // String tagPattern = TAG_PATTERN_DEF;
    //
    // @NonNull
    // @lombok.Builder.Default
    // String versionOverride = "";
    //
    // @NonNull
    // @lombok.Builder.Default
    // String versionPattern = VERSION_PATTERN_DEF;
    //
    // @NonNull
    // @lombok.Builder.Default
    // String releaseBranches = RELEASE_BRANCHES_DEF;

//    @lombok.Builder.Default
//    @NonNull
//    private String branch = "main";
//
//    @lombok.Builder.Default
//    @NonNull
//    private String hash = "abcdef01";
//
//    @lombok.Builder.Default
//    @NonNull
//    private String tagged = "0.0.0";
//
//    private int commits;
//
//    private boolean dirty;

//    @Override
//    public GitverConfig getConfig() {
//        return config;
//    }

    public static PatternStrategy create() {
        return builder().build();
    }

    private String getBranch() {
        return resolved.branch();
    }


    private String getHash() {
        return resolved.hash();
    }

    private String getTagged() {
        return resolved.tagged();
    }

    private int getCommits() {
        return resolved.commits();
    }

    private boolean getDirty() {
        return resolved.dirty();
    }

    public String getHashShort() {
        return resolved.getHashShort();
    }

    @Override
    public String toString() {
        return String.format(
                "%s [branch: %s, version: %s, hash: %s]",
                getClass().getSimpleName(),
                resolved.branch(),
                toVersionString(),
                resolved.hash());
    }

    @Override
    public String toVersionString() {
        return toVersionString(config, resolved);
    }

    public static PatternStrategy from(Map<String, String> props) {
        Map<String, String> resolvedMap = Util.removePrefix(GITTLE_RESOLVED_PREFIX, props);
        resolvedMap.remove(VERSION_STRING);
        Map<String, String> configMap = Util.removePrefix(GITTLE_PREFIX, props);
        return PatternStrategy.builder()
                .setResolved(PropCodec.fromProperties(resolvedMap, ResolvedData.class))
                .setConfig(PropCodec.fromProperties(configMap, GitverConfig.class))
                .build();
    }

    @Override
    public Map<String, String> asMap() {
        Map<String, String> map = new LinkedHashMap<>(getConfig().asMap());
        Map<String, String> resolvedMap = resolved.asMap();
        if (!resolvedMap.isEmpty()) {
            resolvedMap.put(VERSION_STRING, toVersionString());
        }
        map.putAll(Util.appendPrefix(RESOLVED_PREFIX, resolvedMap));
        return Util.appendPrefix(GITTLE_PREFIX, map);
    }

    @Override
    public Properties toProperties() {
        return Util.toProperties(asMap());
    }

    private static String toVersionString(GitverConfig config, ResolvedData resolved) {
        String pattern = config.getVersionPattern();
        Map<String, String> values = getReplacementMap(config, resolved);
        return performTokenReplacements(pattern, values);
    }

    private static Map<String, String> getReplacementMap(GitverConfig config, ResolvedData resolved) {
        Set<String> releaseSet = config.getReleaseBranchesSet();
        String branch = Optional.ofNullable(resolved.branch()).orElse(releaseSet.stream().findFirst().orElse("unknown"));
        String devBranch = releaseSet.contains(branch) ? "" : branch;
        int commits = resolved.commits();
        String hash = resolved.hash();
        boolean dirty = resolved.dirty();
        return Arrays.stream(PatternToken.values())
                .collect(Collectors.toMap(
                        PatternToken::id,
                        t -> String.valueOf(
                                switch (t) {
                                    case TAG -> resolved.tagged();
                                    case COMMIT -> commits;
                                    case SNAPSHOT -> commits > 0 ? "SNAPSHOT" : "";
                                    case BRANCH -> branch;
                                    case DEV_BRANCH -> devBranch;
                                    case HASH_SHORT -> resolved.getHashShort();
                                    case HASH -> hash;
                                    case DIRTY -> dirty ? "dirty" : "";
                                }
                        )
                ));
    }

    private static String performTokenReplacements(String versionPattern, Map<String, String> codeReplMap) {
        String codes =
                Arrays.stream(PatternToken.values()).map(PatternToken::code).collect(Collectors.joining());
        String tokenRegex = quote("%") + "[" + codes + "]";
        Pattern patternx = Pattern.compile("(?<uni>" + tokenRegex + ")"
                + "|\\("
                + "(?<pre>[^()%]+)?(?<mid>" + tokenRegex + ")(?<suf>[^()%]+)?"
                + "\\)");

        Matcher m = patternx.matcher(versionPattern);
        AtomicInteger priorEnd = new AtomicInteger(-1);

        String result = m.results()
                .flatMap(r -> {
                    String uni = null;
                    String pre = null;
                    String mid = null;
                    String suf = null;
                    for (int ii = 1; ii <= r.groupCount(); ii++) {
                        switch (ii) {
                            case 1:
                                uni = r.group(ii);
                                break;
                            case 2:
                                pre = r.group(ii);
                                break;
                            case 3:
                                mid = r.group(ii);
                                break;
                            case 4:
                                suf = r.group(ii);
                                break;
                        }
                    }

                    List<String> res = new LinkedList<>();
                    String tokId = "";
                    if (Util.isNotBlank(uni)) {
                        tokId = uni;
                        res.add(Optional.ofNullable(codeReplMap.get(tokId)).orElse(""));
                    } else if (Util.isNotBlank(mid)) {
                        tokId = mid;
                        String repl =
                                Optional.ofNullable(codeReplMap.get(tokId)).orElse("");
                        if (!repl.isEmpty() && !"0".equals(repl)) {
                            Stream.of(pre, repl, suf).filter(Util::isNotEmpty).forEach(res::add);
                        }
                    }

                    int priorMatchEnd = priorEnd.getAndUpdate($ -> r.end());
                    if (priorMatchEnd > -1 && priorMatchEnd < r.start()) {
                        String unmatchedPreviousSegment = versionPattern.substring(priorMatchEnd, r.start());
                        res.add(0, unmatchedPreviousSegment);
                    }
                    return res.stream();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.joining());

        // append any remaining after the matches ran out
        int lastMatchEndIdx = priorEnd.get();
        result = lastMatchEndIdx < 0 ? result : result.concat(versionPattern.substring(lastMatchEndIdx));
        return result;
    }

    @Getter
    @Accessors(fluent = true)
    public enum PatternToken {
        TAG("t"),
        COMMIT("c"),
        SNAPSHOT("S"),
        BRANCH("b"),
        DEV_BRANCH("B"),
        HASH_SHORT("h"),
        HASH("H"),
        DIRTY("d");

        private final String id;
        private final String code;

        PatternToken(String code) {
            this.id = "%" + code;
            this.code = code;
        }

        @Override
        public String toString() {
            return id();
        }
    }

    public static class Builder {

        @Tolerate
        public Builder setConfig(GitverConfig.Builder builder) {
            return setConfig(builder.build());
        }

        @Tolerate
        public Builder setResolved(ResolvedData.Builder builder) {
            return setResolved(builder.build());
        }

//        private ResolvedData getResolved() {
//            if (!resolved$set) {
//                setResolved(new ResolvedData());
//            }
//            return resolved$value;
//        }
//
//        public Builder setBranch(String v) {
//            getResolved().branch(v);
//            return this;
//        }
//
//        public Builder setHash(String v) {
//            getResolved().hash(v);
//            return this;
//        }
//
//        public Builder setCommits(int v) {
//            getResolved().commits(v);
//            return this;
//        }
//
//        public Builder setDirty(boolean v) {
//            getResolved().dirty(v);
//            return this;
//        }
    }
}
