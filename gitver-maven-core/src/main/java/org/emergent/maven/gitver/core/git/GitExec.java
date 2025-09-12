package org.emergent.maven.gitver.core.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.emergent.maven.gitver.core.GitverException;

@Slf4j
public class GitExec {

  private GitExec() {}

  public static <R> R execOp(Path basePath, Operation<Git, R> work) throws GitverException {
    return execOp(basePath.toFile(), work);
  }

  public static <R> R execOp(File basePath, Operation<Git, R> work) throws GitverException {
    try (Repository repository = getRepository(basePath);
         Git git = new Git(repository)) {
      return work.apply(git);
    } catch (Exception e) {
      throw new GitverException(e);
    }
  }

  public static void execOp(Path basePath, ExecConsumer<Git> work) throws GitverException {
    execOp(basePath.toFile(), work);
  }

  public static void execOp(File basePath, ExecConsumer<Git> work) throws GitverException {
    try (Repository repository = getRepository(basePath);
         Git git = new Git(repository)) {
      work.accept(git);
    } catch (Exception e) {
      throw new GitverException(e);
    }
  }

  public static String findGitDir(File basePath) {
    try (Repository repository = getRepository(basePath)) {
      return repository.getDirectory().getAbsolutePath();
    } catch (Exception e) {
      throw new GitverException(e);
    }
  }

  private static Repository getRepository(File basePath) throws IOException {
    return new FileRepositoryBuilder().readEnvironment().findGitDir(basePath).build();
  }

  @FunctionalInterface
  public interface Operation<T, R> {
    R apply(T t) throws Exception;
  }

  @FunctionalInterface
  public interface ExecConsumer<T> {
    void accept(T t) throws Exception;
  }
}
