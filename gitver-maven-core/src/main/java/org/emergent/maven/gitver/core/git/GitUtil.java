package org.emergent.maven.gitver.core.git;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.emergent.maven.gitver.core.GitverException;

public class GitUtil {

  private static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");
  private static final File NULL_FILE = new File(IS_WINDOWS ? "NUL" : "/dev/null");

  private final Path basePath;
  private final boolean useNative;
  private final Duration timeout;

  private GitUtil(Path basedir) {
    this(basedir, false);
  }

  private GitUtil(Path basedir, boolean useNative) {
    this.basePath = basedir.toAbsolutePath();
    this.useNative = useNative;
    this.timeout = Duration.ofSeconds(5);
  }

  public static GitUtil getInstance(File dir) {
    return getInstance(dir.toPath());
  }

  public static GitUtil getInstance(Path dir) {
    return new GitUtil(dir.toAbsolutePath());
  }

  public String createTag(String tagName, String tagMessage, boolean force) {
    return execOp(git -> {
      Ref tag = git.tag().setName(tagName).setMessage(tagMessage).setForceUpdate(force).call();
      return String.format("%s@%s", tag.getName(), tag.getObjectId().getName());
    });
  }

  public boolean tagExists(String tagName) {
    return execOp(git -> null != git.getRepository().findRef("refs/tags/" + tagName));
  }

  public void executeCommit(String message) {
    if (useNative) {
      executeCommitNative(message);
    } else {
      executeCommitJava(message);
    }
  }

  private void executeCommitJava(String message) {
    execOp(git -> {
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
        .command("git", "--git-dir", findGitDir(), "commit", "--allow-empty", "-m", message)
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

  public <R> R execOp(GitExec.Operation<Git, R> work) throws GitverException {
    return GitExec.execOp(basePath, work);
  }

  public void execOp(GitExec.ExecConsumer<Git> work) throws GitverException {
    GitExec.execOp(basePath, work);
  }

  public String findGitDir() {
    return GitExec.findGitDir(basePath);
  }

  private static RevWalk getWalk(Repository repository, ObjectId headId) throws IOException, NoHeadException {
    if (headId == null)
      throw new NoHeadException(JGitText.get().noHEADExistsAndNoExplicitStartingRevisionWasSpecified);
    RevWalk walk = new RevWalk(repository);
    walk.markStart(walk.lookupCommit(headId));
    return walk;
  }
}
