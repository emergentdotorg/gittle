package org.emergent.maven.gitver.plugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class CommitMojoTest extends AbstractMojoTest {

  @TempDir
  public Path temporaryFolder;

  @ParameterizedTest
  @CsvSource({"commit-patch, patch", "commit-minor, minor", "commit-major, major"})
  public void executeVersionCommit(String goal, String keyword) throws Exception {
    File tempProject = setupTestProject();
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      CommitMojo commit = (CommitMojo)rule.lookupConfiguredMojo(tempProject, goal);
      assertThat(commit).isNotNull();
      commit.execute();
      Iterable<RevCommit> commits = git.log().call();
      List<RevCommit> revCommits =
        StreamSupport.stream(commits.spliterator(), false).collect(Collectors.toList());
      assertThat(revCommits.get(0).getShortMessage())
        .isEqualTo(String.format("chore(release): [%s]", keyword));
    }
  }

  @ParameterizedTest
  @CsvSource({"commit-patch, patch", "commit-minor, minor", "commit-major, major"})
  public void executeVersionCommitCustomMessage(String goal, String keyword) throws Exception {
    File tempProject = setupTestProject();
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      CommitMojo commit = (CommitMojo)rule.lookupConfiguredMojo(tempProject, goal);
      commit.setMessage("chore: releasing [%k]");
      assertThat(commit).isNotNull();
      commit.execute();
      Iterable<RevCommit> commits = git.log().call();
      List<RevCommit> revCommits =
        StreamSupport.stream(commits.spliterator(), false).collect(Collectors.toList());
      assertThat(revCommits.get(0).getShortMessage())
        .isEqualTo(String.format("chore: releasing [%s]", keyword));
    }
  }

  private File setupTestProject() throws IOException {
    File tempProject = temporaryFolder.toFile();
    tempProject.mkdirs();
    Path testProject = Paths.get("src/test/resources/project-to-test/");
    Files.copy(
      testProject.resolve("pom.xml"),
      tempProject.toPath().resolve("pom.xml"),
      StandardCopyOption.REPLACE_EXISTING);
    assertThat(tempProject.list()).contains("pom.xml");
    return tempProject;
  }

  private static Git getMain(File tempProject) throws GitAPIException, IOException {
    Git main = Git.init().setInitialBranch("main").setDirectory(tempProject).call();
    StoredConfig config = main.getRepository().getConfig();
    config.setString("user", null, "name", "GitHub Actions Test");
    config.setString("user", null, "email", "");
    config.save();
    return main;
  }

  private static void addEmptyCommit(Git git) throws GitAPIException {
    git.commit().setSign(false).setMessage("Empty commit").setAllowEmpty(true).call();
  }

  private static String addCommit(Git git, String message) throws GitAPIException {
    RevCommit commit = git.commit().setSign(false).setMessage(message).setAllowEmpty(true).call();
    return commit.toObjectId().getName();
  }
}
