package org.emergent.maven.gitver.plugin;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.emergent.maven.gitver.core.GitVerConfig;
import org.emergent.maven.gitver.core.version.VersionStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractGitverMojoTest {

  private final AbstractGitverMojo testMojo = new AbstractGitverMojo() {

    {
      mavenProject = new MavenProject();
      mavenProject.setFile(new File("my/pom.xml"));
    }

    @Override
    public void execute0() throws MojoExecutionException, MojoFailureException {}
  };

  @Test
  public void getVersionConfig() {
    assertThat(testMojo)
      .isNotNull()
      .extracting("disabled", "versionPattern", "versionOverride")
      .containsExactly(false, GitVerConfig.DEFAULT_VERSION_PATTERN, "");
    assertThat(testMojo).isNotNull();
//    assertThat(testMojo)
//      .extracting("initialMajor", "initialMinor", "initialPatch")
//      .containsExactly(0, 0, 0);
//    assertThat(testMojo)
//      .isNotNull()
//      .extracting("majorKey", "minorKey", "patchKey", "useRegex")
//      .containsExactly(GitVerConfig.KEY_MAJOR, GitVerConfig.KEY_MINOR, GitVerConfig.KEY_PATCH, false);
  }

/*
  @Test
  public void setVersionConfig() {
    AbstractGitverMojo mojo = new AbstractGitverMojo() {
      @Override
      public void execute0() throws MojoExecutionException, MojoFailureException {}
    };
    mojo.setInitialMajor(1);
    assertThat(mojo)
      .isNotNull()
      .extracting("initialMajor", "initialMinor", "initialPatch")
      .containsExactly(1, 0, 0);
    assertThat(testMojo)
      .isNotNull()
      .extracting("majorKey", "minorKey", "patchKey", "useRegex")
      .containsExactly(GitVerConfig.KEY_MAJOR, GitVerConfig.KEY_MINOR, GitVerConfig.KEY_PATCH, false);
  }
*/

  @Test
  public void getVersionStrategy() {
    VersionStrategy versioner = testMojo.getVersionStrategy();
    assertThat(versioner).isNotNull().isInstanceOf(VersionStrategy.class);
  }

}
