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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevTag;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.VersionConfig;
import org.emergent.maven.gitver.core.version.SemVer;
import org.emergent.maven.gitver.core.version.VersionPatternStrategy;
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
      if  (process.exitValue() != 0) {
        throw new GitverException("Git commit returned exit code " + process.exitValue());
      }
    } catch (Exception e) {
      throw new GitverException(e.getMessage(), e);
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

  private static String getGitDir(File mavenBasedir) {
    return GitExec.findGitDir(mavenBasedir.getAbsoluteFile());
  }

  private static String getGitDir(Path mavenBasedir) {
    return GitExec.findGitDir(mavenBasedir.toFile().getAbsoluteFile());
  }

  public VersionStrategy getVersionStrategy(VersionConfig versionConfig) {
    return getVersionStrategy0(basedir, versionConfig);
  }

  public static VersionStrategy getVersionStrategy0(Path basePath, VersionConfig versionConfig) {
    return GitExec.execOp(
      basePath.toAbsolutePath(),
      git -> {

        String branch = git.getRepository().getBranch();
        Ref head = git.getRepository().findRef("HEAD");
        String hash = Optional.ofNullable(head).map(Ref::getObjectId).map(ObjectId::getName).orElse("");

        // create a map of commit-refs and corresponding list of tags
        Map<ObjectId, List<Ref>> tagMap = git.tagList().call().stream()
          .filter(tag -> Util.VERSION_REGEX.asMatchPredicate().test(tag.getName()))
          .collect(Collectors.groupingBy(
            ref -> getObjectId(git.getRepository(), ref),
            Collectors.mapping(ref -> ref, Collectors.toList())
          ));

        VersionPatternStrategy.Builder versionStrategy = VersionPatternStrategy.builder()
          .setBranch(branch)
          .setHash(hash)
          .setVersionConfig(versionConfig)
          .setMajor(versionConfig.getInitialMajor())
          .setMinor(versionConfig.getInitialMinor())
          .setPatch(versionConfig.getInitialPatch());

        Iterable<RevCommit> commits = git.log().call();
        List<RevCommit> revCommits = StreamSupport.stream(commits.spliterator(), false).collect(Collectors.toList());
        Collections.reverse(revCommits);

        for (RevCommit commit : revCommits) {
          String fullMessage = commit.getFullMessage();

          List<Ref> tags = tagMap.getOrDefault(commit.getId(), Collections.emptyList());
          Optional<SemVer> semVer = tags.stream()
            .map(t -> {
              String tagName = t.getName();
              Matcher matcher = Util.VERSION_REGEX.matcher(tagName);
              if (!matcher.matches()) {
                return null;
              }
              return SemVer.builder()
                .setMajor(matcher.group("major"))
                .setMinor(matcher.group("minor"))
                .setPatch(matcher.group("patch"))
                .build();
            })
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder());

          if (semVer.isPresent()) {
            SemVer sv = semVer.get();
            versionStrategy.resetVersion(sv.getMajor(), sv.getMinor(), sv.getPatch());
          } else {
            if (hasMajorKeyword(versionConfig, fullMessage)) {
              versionStrategy.incrementMajor();
            } else if (hasMinorKeyword(versionConfig, fullMessage)) {
              versionStrategy.incrementMinor();
            } else if (hasPatchKeyword(versionConfig, fullMessage)) {
              versionStrategy.incrementPatch();
            } else {
              versionStrategy.incrementCommit();
            }
          }
        }
        return versionStrategy.build();
      });
  }

  static ObjectId getObjectId(Repository repository, Ref ref) {
    return getObjectId(resolveRevObject(repository, ref));
  }

  static ObjectId getObjectId(Repository repository, ObjectId objectId) {
    return getObjectId(resolveRevObject(repository, objectId));
  }

  static RevObject resolveRevObject(Repository repository, Ref ref) {
    return getTargetRevObject(getRevObject(repository, ref));
  }

  static RevObject resolveRevObject(Repository repository, ObjectId objectId) {
    return getTargetRevObject(getRevObject(repository, objectId));
  }

  static RevObject getRevObject(Repository repository, Ref ref) {
    return getRevObject(repository, peeledId(ref));
  }

  static RevObject getRevObject(Repository repository, ObjectId objectId) {
    try (ObjectReader reader = repository.newObjectReader()) {
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

  static ObjectId getObjectId(RevObject revObj) {
    return Optional.ofNullable(revObj).map(RevObject::getId).orElse(null);
  }

  static ObjectId peeledId(Ref ref) {
    Util.check(ref.isPeeled() == (ref.getPeeledObjectId() != null));
    return ref.isPeeled() ? ref.getPeeledObjectId() : ref.getObjectId();
  }

  static boolean hasMajorKeyword(VersionConfig versionConfig, String commitMessage) {
    return hasValue(versionConfig, commitMessage, versionConfig.getMajorKey());
  }

  static boolean hasMinorKeyword(VersionConfig versionConfig, String commitMessage) {
    return hasValue(versionConfig, commitMessage, versionConfig.getMinorKey());
  }

  static boolean hasPatchKeyword(VersionConfig versionConfig, String commitMessage) {
    return hasValue(versionConfig, commitMessage, versionConfig.getPatchKey());
  }

  static boolean hasValue(VersionConfig versionConfig, String commitMessage, String keyword) {
    if (versionConfig.isUseRegex()) {
      return commitMessage.matches(keyword);
    } else {
      return commitMessage.contains(keyword);
    }
  }
}
