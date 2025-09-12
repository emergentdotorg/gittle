package org.emergent.maven.gitver.core.git;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.eclipse.jgit.revwalk.RevWalk;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.VersionConfig;
import org.emergent.maven.gitver.core.version.OverrideStrategy;
import org.emergent.maven.gitver.core.version.SemVer;
import org.emergent.maven.gitver.core.version.PatternStrategy;
import org.emergent.maven.gitver.core.version.VersionStrategy;

public class GitUtil {

  private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

  private static final File NULL_FILE = new File(IS_WINDOWS ? "NUL" : "/dev/null");

  private final Path basedir;
  private final boolean useNative;
  private final Duration timeout;

  private GitUtil(Path basedir) {
    this(basedir, false);
  }

  private GitUtil(Path basedir, boolean useNative) {
    this.basedir = basedir.toAbsolutePath();
    this.useNative = useNative;
    this.timeout = Duration.ofSeconds(5);
  }

  public static GitUtil getInstance(File dir) {
    return getInstance(dir.toPath());
  }

  public static GitUtil getInstance(Path dir) {
    return new GitUtil(dir);
  }

  public String createTag(String tagName, String tagMessage, boolean force) {
    return GitExec.execOp(basedir, git -> {
      Ref tag = git.tag().setName(tagName).setMessage(tagMessage).setForceUpdate(force).call();
      return String.format("%s@%s", tag.getName(), tag.getObjectId().getName());
    });
  }

  public boolean tagExists(String tagName) {
    return GitExec.execOp(basedir, git -> null != git.getRepository().findRef("refs/tags/" + tagName));
  }

  public void executeCommit(String message) {
    if (useNative) {
      executeCommitNative(message);
    } else {
      executeCommitJava(message);
    }
  }

  private void executeCommitJava(String message) {
    GitExec.execOp(basedir.toAbsolutePath(), git -> {
      try (FileOutputStream nos = new FileOutputStream(NULL_FILE, true);
           PrintStream ps = new PrintStream(nos)) {
        RevCommit revCommit = git.commit()
          .setAllowEmpty(true)
          .setMessage(message)
          .setHookErrorStream(ps)
          .setHookOutputStream(ps)
          .call();
      } catch (Exception e) {
        throw new GitverException(e.getMessage(), e);
      }
    });
  }

  private void executeCommitNative(String message) {
    try {
      Process process = new ProcessBuilder()
        .command("git", "--git-dir", getGitDir(basedir), "commit", "--allow-empty", "-m", message)
        .inheritIO()
        .redirectInput(ProcessBuilder.Redirect.from(NULL_FILE))
        .redirectOutput(ProcessBuilder.Redirect.to(NULL_FILE))
        .redirectError(ProcessBuilder.Redirect.to(NULL_FILE))
        .start();
      if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
        throw new GitverException("Timed out while creating commit");
      }
      if (process.exitValue() != 0) {
        throw new GitverException("Git commit returned exit code " + process.exitValue());
      }
    } catch (Exception e) {
      throw new GitverException(e.getMessage(), e);
    }
  }

  private static String getGitDir(File mavenBasedir) {
    return GitExec.findGitDir(mavenBasedir.getAbsoluteFile());
  }

  private static String getGitDir(Path mavenBasedir) {
    return GitExec.findGitDir(mavenBasedir.toFile().getAbsoluteFile());
  }

  public VersionStrategy getVersionStrategy(VersionConfig versionConfig) {
    Optional<VersionStrategy> overrideStrategy = Optional.of(versionConfig.getVersionOverride())
      .filter(Util::isNotEmpty).map(OverrideStrategy::new);
    return overrideStrategy.orElseGet(() -> GitExec.execOp(basedir.toAbsolutePath(), git -> {
      try (Repository repository = git.getRepository();
           ObjectReader reader = repository.newObjectReader()) {

//      Repository repository = git.getRepository();
        String branch = repository.getBranch();
        ObjectId headId = repository.resolve(Constants.HEAD);
        String hash = Optional.ofNullable(headId).map(ObjectId::getName).orElse("");

        // create a map of commit-refs and corresponding list of tags

        Map<ObjectId, List<Ref>> tagMap = git.tagList().call().stream()
          .filter(tag -> Util.VERSION_REGEX.asMatchPredicate().test(tag.getLeaf().getName()))
          .collect(Collectors.groupingBy(
            ref -> getObjectId(reader, ref),
            Collectors.mapping(ref -> ref, Collectors.toList())
          ));

        PatternStrategy.Builder strategy = PatternStrategy.builder()
          .setVersionConfig(versionConfig)
          .setBranch(branch)
          .setHash(hash)
          .setMajor(versionConfig.getInitialMajor())
          .setMinor(versionConfig.getInitialMinor())
          .setPatch(versionConfig.getInitialPatch());


        LinkedList<RevCommit> revCommits = new LinkedList<>();

        try (RevWalk revWalk = getWalk(repository, headId)) {
          for (RevCommit commit : revWalk) {
            Optional<SemVer> tag = tagMap.getOrDefault(commit.getId(), Collections.emptyList()).stream()
              .map(t -> Optional.of(Util.VERSION_REGEX.matcher(t.getName()))
                .filter(Matcher::matches)
                .map(m -> SemVer.builder()
                  .setMajor(m.group("major"))
                  .setMinor(m.group("minor"))
                  .setPatch(m.group("patch"))
                  .build()
                ).orElse(null))
              .filter(Objects::nonNull)
              .max(Comparator.naturalOrder());

            tag.ifPresent(sv -> strategy.setCommit(0)
              .setMajor(sv.getMajor())
              .setMinor(sv.getMinor())
              .setPatch(sv.getPatch()));

            if (tag.isPresent()) {
              break;
            } else {
              revCommits.addFirst(commit);
            }
          }
        }

        for (RevCommit commit : revCommits) {
          String fullMessage = commit.getFullMessage();
          if (hasMajorKeyword(versionConfig, fullMessage)) {
            strategy.incrementMajor();
          } else if (hasMinorKeyword(versionConfig, fullMessage)) {
            strategy.incrementMinor();
          } else if (hasPatchKeyword(versionConfig, fullMessage)) {
            strategy.incrementPatch();
          } else {
            strategy.incrementCommit();
          }
        }

        return strategy.build();
      }
    }));
  }

  private static RevWalk getWalk(Repository repository, ObjectId headId) throws IOException, NoHeadException {
    if (headId == null)
      throw new NoHeadException(JGitText.get().noHEADExistsAndNoExplicitStartingRevisionWasSpecified);
    RevWalk walk = new RevWalk(repository);
    walk.markStart(walk.lookupCommit(headId));
    return walk;
  }

  static ObjectId getObjectId(ObjectReader reader, Ref ref) {
    return getObjectId(resolveRevObject(reader, ref));
  }

  static ObjectId getObjectId(ObjectReader reader, ObjectId objectId) {
    return getObjectId(resolveRevObject(reader, objectId));
  }

  static RevObject resolveRevObject(ObjectReader reader, Ref ref) {
    return getTargetRevObject(getRevObject(reader, ref));
  }

  static RevObject resolveRevObject(ObjectReader reader, ObjectId objectId) {
    return getTargetRevObject(getRevObject(reader, objectId));
  }

  static RevObject getRevObject(ObjectReader reader, Ref ref) {
    return getRevObject(reader, getObjectId(ref));
  }

  static RevObject getRevObject(ObjectReader reader, ObjectId objectId) {
//    try (ObjectReader reader = repository.newObjectReader()) {
    try {
      ObjectLoader loader = reader.open(objectId);
      int objectType = loader.getType();
      byte[] rawData = loader.getBytes();
      switch (objectType) {
        case Constants.OBJ_EXT:
          System.out.printf("\text: %s%n", objectId.getName());
          return null;
        case Constants.OBJ_COMMIT:
          return RevCommit.parse(rawData);
        case Constants.OBJ_TREE:
          System.out.printf("\ttree: %s%n", objectId.getName());
          return null;
        case Constants.OBJ_BLOB:
          String content = new String(rawData, StandardCharsets.UTF_8);
          System.out.printf("\tblob: %s, content: %s%n", objectId.getName(), content);
          return null;
        case Constants.OBJ_TAG:
          return RevTag.parse(rawData);
        default:
          System.out.printf("\tother: %s%n", objectId.getName());
          return null;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  static ObjectId getObjectId(Ref ref) {
    Util.check(ref.isPeeled() == (ref.getPeeledObjectId() != null));
    return ref.isPeeled() ? ref.getPeeledObjectId() : ref.getObjectId();
  }

  static ObjectId getObjectId(RevObject revObj) {
    return Optional.ofNullable(revObj).map(RevObject::getId).orElse(null);
  }

  static RevObject getTargetRevObject(RevObject revObj) {
    return getTargetRevObject(revObj, true);
  }

  static RevObject getTargetRevObject(RevObject revObj, boolean recursive) {
    return switch (revObj.getType()) {
      case Constants.OBJ_COMMIT -> revObj;
      case Constants.OBJ_TAG -> {
        RevObject target = ((RevTag)revObj).getObject();
        if (!recursive) {
          yield target;
        } else {
          yield getTargetRevObject(target, true);
        }
      }
      default -> null;
    };
  }

  static boolean hasMajorKeyword(VersionConfig config, String message) {
    return hasValue(config, message, config.getMajorKeywordsList());
  }

  static boolean hasMinorKeyword(VersionConfig config, String message) {
    return hasValue(config, message, config.getMinorKeywordsList());
  }

  static boolean hasPatchKeyword(VersionConfig config, String message) {
    return hasValue(config, message, config.getPatchKeywordsList());
  }

  static boolean hasValue(VersionConfig versionConfig, String message, String keyword) {
    return hasValue(versionConfig.isRegexKeywords(), message, Collections.singletonList(keyword));
  }

  static boolean hasValue(VersionConfig versionConfig, String message, List<String> keywords) {
    return hasValue(versionConfig.isRegexKeywords(), message, keywords);
  }

  static boolean hasValue(boolean useRegex, String message, List<String> keywords) {
    if (useRegex) {
      return keywords.stream().anyMatch(message::matches);
    } else {
      return keywords.stream().anyMatch(message::contains);
    }
  }

}
