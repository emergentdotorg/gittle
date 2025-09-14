package org.emergent.maven.gitver.core.version;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.git.GitExec;
import org.emergent.maven.gitver.core.git.TagProvider;

public class StrategyFactory {

    public static VersionStrategy getVersionStrategy(File basePath, GitverConfig config) {
        return getOverrideStrategy(config)
                .orElseGet(() -> GitExec.execOp(basePath, git -> {
                    return getPatternStrategy(config, git);
                }));
    }

    private static Optional<VersionStrategy> getOverrideStrategy(GitverConfig config) {
        return Optional.of(config.getVersionOverride()).filter(Util::isNotEmpty).map(OverrideStrategy::new);
    }

    private static VersionStrategy getPatternStrategy(GitverConfig config, Git git)
            throws GitAPIException, IOException {
        Repository repository = git.getRepository();
        TagProvider tagProvider = new TagProvider(git);
        ObjectId headId = requireNonNull(repository.resolve(Constants.HEAD), "headId is null");
        RefData refData = RefData.builder()
                .setBranch(repository.getBranch())
                .setHash(headId.getName())
                .build();
        BasicVersion.Builder builder = config.getInitial().toBuilder();
        LinkedList<RevCommit> revCommits = new LinkedList<>();
        // We start walking commits newest to oldest until we come to a tag.
        // Once we have a tag we are able to set all the version elements, and short-circuit our walk.
        for (RevCommit commit : git.log().add(headId).call()) {
            LinkedList<BasicVersion> desceningTags = tagProvider.getDescendingTags(commit);
            if (!desceningTags.isEmpty()) {
                builder.reset(desceningTags.getFirst());
                // we found our starting point, so we stop going back into history
                break;
            } else {
                revCommits.addFirst(commit);
            }
        }
        // Now we walk from oldest (immediately after the tag we found) to latest (HEAD),
        // incrementing the various version components based on keywords as we go.
        for (RevCommit commit : revCommits) {
            VersionIncrementType inc = VersionIncrementType.COMMIT;
            boolean isMergeCommit = commit.getParentCount() > 1;
            if (!isMergeCommit) {
                inc = getKeywordIncrement(config.getKeywords(), commit.getFullMessage());
            }
            builder.increment(inc);
        }
        return PatternStrategy.builder()
                .setConfig(config)
                .setRefData(refData)
                .setResolved(builder.build())
                .build();
    }

    private static VersionIncrementType getKeywordIncrement(KeywordsConfig config, String fullMessage) {
        boolean regex = config.isRegex();
        if (hasValue(regex, fullMessage, config.getMajor())) {
            return VersionIncrementType.MAJOR;
        } else if (hasValue(regex, fullMessage, config.getMinor())) {
            return VersionIncrementType.MINOR;
        } else if (hasValue(regex, fullMessage, config.getPatch())) {
            return VersionIncrementType.PATCH;
        } else {
            return VersionIncrementType.COMMIT;
        }
    }

    static boolean hasValue(KeywordsConfig config, String message, String keyword) {
        return hasValue(config.isRegex(), message, Collections.singletonList(keyword));
    }

    static boolean hasValue(boolean useRegex, String message, String keywords) {
        return hasValue(useRegex, message, Arrays.asList(StringUtils.split(keywords, ",")));
    }

    static boolean hasValue(boolean useRegex, String message, List<String> keywords) {
        if (useRegex) {
            return keywords.stream().anyMatch(message::matches);
        } else {
            return keywords.stream().anyMatch(message::contains);
        }
    }
}
