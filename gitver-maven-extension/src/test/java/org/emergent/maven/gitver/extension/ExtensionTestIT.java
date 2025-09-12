package org.emergent.maven.gitver.extension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.apache.commons.io.FileUtils;
import org.apache.maven.it.Verifier;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.emergent.maven.gitver.core.Util;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

public class ExtensionTestIT {

  @TempDir
  public Path temporaryFolder;

  @BeforeEach
  public void housekeeping() {
    // IT tests do not print to standard maven logs
    // It may look like process is stuck until all tests are executed in background.
    System.out.print(".");
  }

  @AfterAll
  public static void cleanHousekeeping() {
    System.out.println(".");
    System.out.println("IT Log files are available in target/it-logs/");
  }

  @Test
  public void extensionBuildInitialVersion() throws Exception {
    File tempProject = setupTestProject();
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
      verifier.displayStreamBuffers();
      verifier.executeGoal("verify");
      copyExecutionLog(tempProject, verifier, "extensionBuildInitialVersion.log.txt");
      verifier.verifyErrorFreeLog();
      String expectedVersion = "0.0.0-1-SNAPSHOT";
      verifier.verifyTextInLog("Building versioner-maven-extension-test " + expectedVersion);
      verifier.verifyTextInLog("versioner-maven-extension-test-" + expectedVersion + ".jar");
      assertThat(resolveGitVerPom(tempProject).toFile())
        .as("Git versioner pom file")
        .exists();
    }
  }

  @Test
  public void extensionBuildVersionWithCommits() throws Exception {
    File tempProject = setupTestProject();
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      addEmptyCommit(git);
      addEmptyCommit(git);
      Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
      verifier.displayStreamBuffers();
      verifier.executeGoal("verify");
      copyExecutionLog(tempProject, verifier, "extensionBuildVersionWithCommits.log.txt");
      verifier.verifyErrorFreeLog();
      String expectedVersion = "0.0.0-3-SNAPSHOT";
      verifier.verifyTextInLog("Building versioner-maven-extension-test " + expectedVersion);
      verifier.verifyTextInLog("versioner-maven-extension-test-" + expectedVersion + ".jar");
      assertThat(resolveGitVerPom(tempProject).toFile())
        .as("Git versioner pom file")
        .exists();
    }
  }

  @Test
  public void extensionValidateVersionProperties() throws Exception {
    File tempProject = setupTestProject();
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      addCommit(git, "[patch]");
      Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
      verifier.displayStreamBuffers();
      verifier.executeGoal("verify");
      copyExecutionLog(tempProject, verifier, "extensionValidateVersionProperties.log.txt");
      verifier.verifyErrorFreeLog();
      String expectedVersion = "0.0.1";
      verifier.verifyTextInLog("Building versioner-maven-extension-test " + expectedVersion);
      verifier.verifyTextInLog("versioner-maven-extension-test-" + expectedVersion + ".jar");
      verifier.verifyTextInLog("gitver.commitNumber=0");
      verifier.verifyTextInLog("gitver.major=0");
      verifier.verifyTextInLog("gitver.minor=0");
      verifier.verifyTextInLog("gitver.patch=1");
      verifier.verifyTextInLog("gitver.version=0.0.1");
      verifier.verifyTextInLog("gitver.branch=");
      verifier.verifyTextInLog("gitver.hash=");
      verifier.verifyTextInLog("gitver.hash.short=");
    }
  }

  @Test
  public void extensionBuildPatchVersion() throws Exception {
    File tempProject = setupTestProject();
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      addCommit(git, "[patch]");
      Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
      verifier.displayStreamBuffers();
      verifier.executeGoal("verify");
      copyExecutionLog(tempProject, verifier, "extensionBuildPatchVersion.log.txt");
      verifier.verifyErrorFreeLog();
      String expectedVersion = "0.0.1";
      verifier.verifyTextInLog("Building versioner-maven-extension-test " + expectedVersion);
      verifier.verifyTextInLog("versioner-maven-extension-test-" + expectedVersion + ".jar");
    }
  }

  @Test
  public void extensionBuildMinorVersion() throws Exception {
    File tempProject = setupTestProject();
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      addCommit(git, "[minor]");
      Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
      verifier.displayStreamBuffers();
      verifier.executeGoal("verify");
      copyExecutionLog(tempProject, verifier, "extensionBuildMinorVersion.log.txt");
      verifier.verifyErrorFreeLog();
      String expectedVersion = "0.1.0";
      verifier.verifyTextInLog("Building versioner-maven-extension-test " + expectedVersion);
      verifier.verifyTextInLog("versioner-maven-extension-test-" + expectedVersion + ".jar");
    }
  }

  @Test
  public void extensionBuildMajorVersion() throws Exception {
    File tempProject = setupTestProject();
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      addCommit(git, "[major]");
      Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
      verifier.displayStreamBuffers();
      verifier.executeGoal("verify");
      copyExecutionLog(tempProject, verifier, "extensionBuildMajorVersion.log.txt");
      verifier.verifyErrorFreeLog();
      String expectedVersion = "1.0.0";
      verifier.verifyTextInLog("Building versioner-maven-extension-test " + expectedVersion);
      verifier.verifyTextInLog("versioner-maven-extension-test-" + expectedVersion + ".jar");
    }
  }

  @Test
  public void extensionBuildTagVersion() throws Exception {
    File tempProject = setupTestProject();
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      addTag(git, "v2.3.4");
      addCommit(git, "[patch]");
      Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
      verifier.displayStreamBuffers();
      verifier.executeGoal("verify");
      copyExecutionLog(tempProject, verifier, "extensionBuildTagVersion.log.txt");
      verifier.verifyErrorFreeLog();
      String expectedVersion = "2.3.5";
      verifier.verifyTextInLog("Building versioner-maven-extension-test " + expectedVersion);
      verifier.verifyTextInLog("versioner-maven-extension-test-" + expectedVersion + ".jar");
    }
  }

  @Test
  public void extensionBuildHashVersion() throws Exception {
    File tempProject = setupTestProject(true, "3.");
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      addCommit(git, "[patch]");
      String hash = addCommit(git, "new commit");
      Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
      verifier.displayStreamBuffers();
      verifier.executeGoal("verify");
      copyExecutionLog(tempProject, verifier, "extensionBuildHashVersion.log.txt");
      verifier.verifyErrorFreeLog();
      String expectedVersion = "1.3.5+" + hash.substring(0, 8);
      verifier.verifyTextInLog("Building versioner-maven-extension-test " + expectedVersion);
      verifier.verifyTextInLog("versioner-maven-extension-test-" + expectedVersion + ".jar");
    }
  }

  @Test
  public void extensionWithInitialVersionProperties() throws Exception {
    File tempProject = setupTestProject(true, "1.");
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      addCommit(git, "[patch]");
      String hash = addCommit(git, "new commit");
      Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
      verifier.displayStreamBuffers();
      verifier.executeGoal("verify");
      copyExecutionLog(tempProject, verifier, "extensionWithInitialVersionProperties.log.txt");
      verifier.verifyErrorFreeLog();
      String expectedVersion = "1.3.5-1";
      verifier.verifyTextInLog("Building versioner-maven-extension-test " + expectedVersion);
      verifier.verifyTextInLog("versioner-maven-extension-test-" + expectedVersion + ".jar");
    }
  }

  @Test
  public void extensionWithModule() throws Exception {
    File tempProject = temporaryFolder.resolve("test").toFile();
    FileUtils.copyDirectory(
      Paths.get(getResourcesPrefix() + "multi-module-project").toFile(), tempProject);
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      addCommit(git, "[patch]");
      Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
      verifier.displayStreamBuffers();
      verifier.executeGoal("verify");
      copyExecutionLog(tempProject, verifier, "extensionWithModule.log.txt");
      verifier.verifyErrorFreeLog();
      String expectedVersion = "0.0.1";
      verifier.verifyTextInLog("Building multi-module-parent " + expectedVersion);
      verifier.verifyTextInLog("Building cli " + expectedVersion);
      verifier.verifyTextInLog("Building lib " + expectedVersion);
    }
  }

  @Test
  public void extensionWithParentChild() throws Exception {
    File tempProject = temporaryFolder.resolve("test").toFile();
    FileUtils.copyDirectory(
      Paths.get(getResourcesPrefix() + "parent-child-project").toFile(), tempProject);
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      addCommit(git, "[patch]");
      Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
      verifier.displayStreamBuffers();
      verifier.executeGoal("verify");
      copyExecutionLog(tempProject, verifier, "extensionWithParentChild.log.txt");
      verifier.verifyErrorFreeLog();
      String expectedVersion = "0.0.1";
      verifier.verifyTextInLog("Building parent-test-pom " + expectedVersion);
      verifier.verifyTextInLog("Building cli " + expectedVersion);
      verifier.verifyTextInLog(
        "Setting parent org.emergent.maven.gitver.its:parent-test-pom:pom:0 version to " + expectedVersion);
      verifier.verifyTextInLog("Building lib " + expectedVersion);
    }
  }

  @ParameterizedTest
  @CsvSource({"[BIG], 1.0.0", "[MEDIUM], 0.1.0", "[SMALL], 0.0.1"})
  public void extensionWithVersionKeywordProperties(String key, String expectedVersion) throws Exception {
    File tempProject = setupTestProject(true, "2.");
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      addCommit(git, key);
      Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
      verifier.displayStreamBuffers();
      verifier.executeGoal("verify");
      copyExecutionLog(
        tempProject, verifier, "extensionWithVersionKeywordProperties" + key + ".log.txt");
      verifier.verifyErrorFreeLog();
      verifier.verifyTextInLog("Building versioner-maven-extension-test " + expectedVersion);
    }
  }

  @ParameterizedTest
  @CsvSource({"Empty, 1.3.4-2", "[patch], 1.3.5"})
  public void extensionWithCommitsProperties(String key, String expectedVersion) throws Exception {
    File tempProject = setupTestProject(true, "1.");
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      addCommit(git, key);
      Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
      verifier.displayStreamBuffers();
      verifier.executeGoal("verify");
      copyExecutionLog(tempProject, verifier, "extensionWithCommitsProperties" + key + ".log.txt");
      verifier.verifyErrorFreeLog();
      verifier.verifyTextInLog("Building versioner-maven-extension-test " + expectedVersion);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "[BIG], 1.0.0, commit-major",
    "[MEDIUM], 0.1.0, commit-minor",
    "[SMALL], 0.0.1, commit-patch"
  })
  public void extensionWithVersionKeyword_AddCommits(String key, String expectedVersion, String goal) throws Exception {
    File tempProject = setupTestProject(true, "2.");
    try (Git git = getMain(tempProject)) {
      addEmptyCommit(git);
      Verifier verifier = new Verifier(tempProject.getAbsolutePath(), true);
      verifier.displayStreamBuffers();
      verifier.executeGoal("gitver:" + goal);
      //verifier.executeGoal("clean");
      verifier.executeGoal("verify");
      copyExecutionLog(
        tempProject, verifier, "extensionWithVersionKeyword_AddCommits" + key + ".log.txt");
      verifier.verifyErrorFreeLog();
      verifier.verifyTextInLog("Building versioner-maven-extension-test " + expectedVersion);
    }
  }

  private static void copyExecutionLog(File tempProject, Verifier verifier, String logName) throws IOException {
    Path destdir = Files.createDirectories(Paths.get("./target/it-logs/"));
    Path target = destdir.resolve(logName);
    if (target.toFile().exists()) target.toFile().delete();
    Files.copy(tempProject.toPath().resolve(verifier.getLogFileName()), target);
    //String altpomname = ".gitver.pom.xml";
    //Files.copy(tempProject.toPath().resolve(altpomname), destdir.resolve(logName + ".pom.xml"));
    //Path propsfile = tempProject.toPath().resolve(".mvn/gitver-maven-extension.properties");
    //if (Files.exists(propsfile)) {
    //  Files.copy(propsfile, destdir.resolve(logName + ".extension.properties"));
    //}
  }

  private static void addEmptyCommit(Git git) throws GitAPIException {
    git.commit().setSign(false).setMessage("Empty commit").setAllowEmpty(true).call();
  }

  private static String addCommit(Git git, String message) throws GitAPIException {
    RevCommit commit = git.commit().setSign(false).setMessage(message).setAllowEmpty(true).call();
    return commit.toObjectId().getName();
  }

  private static String addTag(Git git, String message) throws GitAPIException {
    Ref commit = git.tag().setName(message).call();
    return commit.getLeaf().getName();
  }

  private static Git getMain(File tempProject) throws GitAPIException, IOException {
    Git main = Git.init().setInitialBranch("main").setDirectory(tempProject).call();
    StoredConfig config = main.getRepository().getConfig();
    config.setString("user", null, "name", "GitHub Actions Test");
    config.setString("user", null, "email", "");
    config.save();
    return main;
  }

  private File setupTestProject() throws IOException {
    return setupTestProject(false, "");
  }

  private File setupTestProject(boolean copyProperties, String propPrefix) throws IOException {
    File tempProject = temporaryFolder.toFile();
    tempProject.mkdirs();
    Path testProject = Paths.get(getResourcesPrefix() + "project-with-extension");
    Files.copy(
      testProject.resolve("pom.xml"),
      tempProject.toPath().resolve("pom.xml"),
      StandardCopyOption.REPLACE_EXISTING);
    Path mvnDir = tempProject.toPath().resolve(Util.DOT_MVN);
    Files.createDirectory(mvnDir);
    Files.copy(
      testProject.resolve(Util.DOT_MVN).resolve("extensions.xml"),
      mvnDir.resolve("extensions.xml"),
      StandardCopyOption.REPLACE_EXISTING);
    if (copyProperties) {
      Files.copy(
        testProject
          .resolve(Util.DOT_MVN)
          .resolve(propPrefix.concat(Util.GITVER_EXTENSION_PROPERTIES)),
        mvnDir.resolve(Util.GITVER_EXTENSION_PROPERTIES),
        StandardCopyOption.REPLACE_EXISTING);
      assertThat(mvnDir.toFile().list()).contains(Util.GITVER_EXTENSION_PROPERTIES);
    }
    assertThat(tempProject.list()).contains("pom.xml");
    assertThat(mvnDir.toFile().list()).contains("extensions.xml");
    return tempProject;
  }

  private String getResourcesPrefix() {
    //    return "src/test/resources";
    return "target/test-classes/";
  }

  private static Path resolveGitVerPom(File basedir) {
    return resolveGitVerPom(basedir.toPath());
  }

  private static Path resolveGitVerPom(Path basedir) {
    return basedir.resolve(Util.GITVER_POM_XML);
  }

}
