package org.emergent.maven.gitver.core.version;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.submodule.SubmoduleWalk;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.PropCodec;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.git.GitExec;
import org.emergent.maven.gitver.core.git.TagProvider;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.quote;

@Value
@NonFinal
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = false)
@lombok.experimental.FieldDefaults(level = AccessLevel.PROTECTED)
@lombok.experimental.Accessors(fluent = false)
public class PatternStrategy extends ResolvedData implements VersionStrategy {
    public static final String VERSION_PATTERN_DEF = "%t(-%B)(-%c)(-%S)+%h(.%d)";

    private static final String STANDARD_PREFIX = "gittle.resolved.";
    private static final String VERSION_STRING = "version";
    private static final String RESOLVED_PREFIX = "resolved.";

//    @lombok.Builder.ObtainVia(method = "getInitialBuilderVersion", isStatic = true)
//    @Getter(value =  AccessLevel.PRIVATE)
    String version;

    public String getVersion() {
        return Util.isEmpty(version) ? "" : version;
    }

    static VersionStrategy getPatternStrategy(GitverConfig config, File basePath) {
        return GitExec.execOp(basePath, git -> {
            return getPatternStrategy(config, git);
        });
    }

    private static VersionStrategy getPatternStrategy(GitverConfig config, Git git) throws Exception {
        Repository repository = git.getRepository();
        TagProvider tagProvider = new TagProvider(config, git);
        ObjectId headId = requireNonNull(repository.resolve(Constants.HEAD), "headId is null");

        PatternStrategyBuilder<?, ?> builder = builder()
                .newVersion(config.getNewVersion())
                .releaseBranches(config.getReleaseBranches())
                .tagNamePattern(config.getTagNamePattern())
                .versionPattern(config.getVersionPattern())
                .branch(repository.getBranch())
                .hash(headId.getName());

        int commits = 0;
        for (RevCommit commit : git.log().add(headId).call()) {
            Optional<String> tag = tagProvider.getTag(commit).map(ComparableVersion::toString);
            if (tag.isPresent()) {
                builder.tagged(tag.get());
                break;
            }
            boolean isMergeCommit = commit.getParentCount() > 1;
            commits++;
        }
        builder.commits(commits);

        Status status = git.status().setIgnoreSubmodules(SubmoduleWalk.IgnoreSubmoduleMode.UNTRACKED).call();
        builder.dirty(!status.getUncommittedChanges().isEmpty());

        return builder().build();
    }

//    public PatternStrategy roundTrip() {
//        String versionString = calculateVersion(this);
//        return new PatternStrategy(toBuilder().version(versionString));
//    }

//    @Override
//    public String toString() {
//        return String.format(
//                "%s [branch: %s, version: %s, hash: %s]",
//                getClass().getSimpleName(),
//                getBranch(),
//                toVersionString(),
//                getHash());
//    }

    @Override
    public String toVersionString() {
        return getVersion();
    }

    @Override
    public Map<String, String> asMap() {
        return PropCodec.toProperties(this);
    }

    private static String calculateVersion(ResolvedData resolved) {
        String pattern = resolved.getVersionPattern();
        Map<String, String> values = getReplacementMap(resolved);
        return performTokenReplacements(pattern, values);
    }

    private static Map<String, String> getReplacementMap(ResolvedData resolved) {
        Set<String> releaseSet = resolved.getReleaseBranchesSet();
        String branch = Optional.ofNullable(resolved.getBranch()).orElse(releaseSet.stream().findFirst().orElse("unknown"));
        String devBranch = releaseSet.contains(branch) ? "" : branch;
        int commits = resolved.getCommits();
        String hash = resolved.getHash();
        boolean dirty = resolved.isDirty();
        return Arrays.stream(PatternToken.values())
                .collect(Collectors.toMap(
                        PatternToken::id,
                        t -> String.valueOf(
                                switch (t) {
                                    case TAG -> resolved.getTagged();
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

//    public static abstract class PatternStrategyBuilder<C extends PatternStrategy, B extends PatternStrategyBuilder<C, B>> extends ResolvedDataBuilder<C, B> {
//        private String version;
//
//        private static void $fillValuesFromInstanceIntoBuilder(PatternStrategy instance, PatternStrategyBuilder<?, ?> b) {
//            b.version(PatternStrategy.toVersionString0(instance));
//        }
//
//        public B version(String version) {
//            this.version = version;
//            return self();
//        }
//
//        protected B $fillValuesFrom(C instance) {
//            super.$fillValuesFrom(instance);
//            PatternStrategyBuilder.$fillValuesFromInstanceIntoBuilder(instance, this);
//            return self();
//        }
//
//        protected abstract B self();
//
//        public abstract C build();
//
//        public String toString() {
//            return "PatternStrategy.PatternStrategyBuilder(super=" + super.toString() + ", version=" + this.version + ")";
//        }
//    }

    private static final class PatternStrategyBuilderImpl extends PatternStrategyBuilder<PatternStrategy, PatternStrategyBuilderImpl> {
//        private PatternStrategyBuilderImpl() {
//        }
//
//        protected PatternStrategyBuilderImpl self() {
//            return this;
//        }
//
        public PatternStrategy build() {
            return new PatternStrategy(version(calculateVersion(new ResolvedData(this))));
        }
    }
}
