package org.emergent.maven.gitver.core.version;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.Optional;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.submodule.SubmoduleWalk.IgnoreSubmoduleMode;
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
        return Optional.ofNullable(config)
                .filter(c -> Util.isNotEmpty(c.getVersionOverride()))
                .map(OverrideStrategy::new)
                .map(s -> s);
    }

    private static VersionStrategy getPatternStrategy(GitverConfig config, Git git) throws Exception {
        Repository repository = git.getRepository();
        TagProvider tagProvider = new TagProvider(config, git);
        ObjectId headId = requireNonNull(repository.resolve(Constants.HEAD), "headId is null");
        PatternStrategy.Builder builder = PatternStrategy.builder().setConfig(config);
        int commits = 0;
        for (RevCommit commit : git.log().add(headId).call()) {
            Optional<String> tag = tagProvider.getTag(commit).map(ComparableVersion::toString);
            if (tag.isPresent()) {
                builder.setTag(tag.get());
                break;
            }
            boolean isMergeCommit = commit.getParentCount() > 1;
            commits++;
        }
        builder.setCommits(commits);
        builder.setRef(RefData.builder()
                .setBranch(repository.getBranch())
                .setHash(headId.getName())
                .build());
        Status status =
                git.status().setIgnoreSubmodules(IgnoreSubmoduleMode.UNTRACKED).call();
        builder.setDirty(!status.getUncommittedChanges().isEmpty());
        return builder.build();
    }
}
