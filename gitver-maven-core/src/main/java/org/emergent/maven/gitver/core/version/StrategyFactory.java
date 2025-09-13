package org.emergent.maven.gitver.core.version;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.git.GitExec;
import org.emergent.maven.gitver.core.git.TagProvider;

public class StrategyFactory {

  private final File basePath;

  private StrategyFactory(File basePath) {
    this.basePath = basePath.getAbsoluteFile();
  }

  public static StrategyFactory getInstance(Path basePath) {
    return getInstance(basePath.toFile());
  }

  public static StrategyFactory getInstance(File basePath) {
    return new StrategyFactory(basePath);
  }

  public VersionStrategy getVersionStrategy(GitverConfig config) {
    Optional<VersionStrategy> overrideStrategy = Optional.of(config.getVersionOverride())
      .filter(Util::isNotEmpty).map(OverrideStrategy::new);

    //      Repository repository = git.getRepository();
// create a map of commit-refs and corresponding list of tags
// not a merge commit

    return overrideStrategy.orElseGet(() -> GitExec.execOp(basePath, git -> {
      Repository repository = git.getRepository();
      ObjectId headId = Objects.requireNonNull(repository.resolve(Constants.HEAD), "headId is null");
      TagProvider tagProvider = TagProvider.create(git);

      BasicVersion.Builder version = config.getInitial().toBuilder();

      LinkedList<RevCommit> revCommits = new LinkedList<>();
      // We start walking commits newest to oldest until we come to a tag.
      // Once we have a tag we are able to set all the version elements, and short-circuit our walk.

      for (RevCommit commit : git.log().add(headId).call()) {
        // highest semver is first in list
        LinkedList<BasicVersion> referencingTags = tagProvider.getMatchingTags(commit);
        if (!referencingTags.isEmpty()) {
          version.reset(referencingTags.getFirst());
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
        version.increment(inc);
      }

      return PatternStrategy.builder()
        .setConfig(config)
        .setRefData(RefData.builder()
          .setBranch(repository.getBranch())
          .setHash(headId.getName())
          .setVersion(version.build())
          .build())
        .build();
    }));
  }

  private static VersionIncrementType getKeywordIncrement(KeywordsConfig config, String fullMessage) {
    boolean regex = config.isRegexKeywords();
    if (hasValue(regex, fullMessage, config.getMajorKeywords())) {
      return VersionIncrementType.MAJOR;
    } else if (hasValue(regex, fullMessage, config.getMinorKeywords())) {
      return VersionIncrementType.MINOR;
    } else if (hasValue(regex, fullMessage, config.getPatchKeywords())) {
      return VersionIncrementType.PATCH;
    } else {
      return VersionIncrementType.COMMIT;
    }
  }

  static boolean hasValue(KeywordsConfig config, String message, String keyword) {
    return hasValue(config.isRegexKeywords(), message, Collections.singletonList(keyword));
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
