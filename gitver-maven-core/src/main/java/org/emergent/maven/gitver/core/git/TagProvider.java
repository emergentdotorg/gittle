package org.emergent.maven.gitver.core.git;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.emergent.maven.gitver.core.version.BasicVersion;

import static org.emergent.maven.gitver.core.Util.VERSION_REGEX;

public class TagProvider {

  private static final Predicate<String> TAG_REF_NAME_PREDICATE = VERSION_REGEX.asMatchPredicate();
  private static final Predicate<Ref> TAG_REF_PREDICATE = ref -> TAG_REF_NAME_PREDICATE.test(ref.getLeaf().getName());

  Map<ObjectId, List<Ref>> tagMap;

  public static TagProvider create(Git git) throws GitAPIException {
    ObjectResolver resolver = new ObjectResolver(git.getRepository());
    // create a map of commit-refs and corresponding list of tags
    Map<ObjectId, List<Ref>> tagMap = git.tagList().call().stream().filter(TAG_REF_PREDICATE)
      .collect(Collectors.groupingBy(resolver::getObjectId, Collectors.toList()));
    return new TagProvider(tagMap);
  }

  public TagProvider(Map<ObjectId, List<Ref>> tagMap) {
    this.tagMap = tagMap;
  }

  /**
   * Returns a map with highest versions first.
   */
  public LinkedList<BasicVersion> getMatchingTags(RevCommit commit) {
    return tagMap.getOrDefault(commit.getId(), Collections.emptyList()).stream()
      .map(this::getSimpleVer)
      .filter(BasicVersion::isNonZero)
      .sorted(BasicVersion.COMPARATOR.reversed())
      .collect(Collectors.toCollection(LinkedList::new));
  }

  private BasicVersion getSimpleVer(Ref tag) {
    BasicVersion.Builder builder = BasicVersion.builder();
    Matcher matcher = VERSION_REGEX.matcher(tag.getLeaf().getName());
    if (matcher.matches()) {
      builder
        .setMajor(Integer.parseInt(matcher.group("major")))
        .setMinor(Integer.parseInt(matcher.group("minor")))
        .setPatch(Integer.parseInt(matcher.group("patch")));
    }
    return builder.build();
  }
}
