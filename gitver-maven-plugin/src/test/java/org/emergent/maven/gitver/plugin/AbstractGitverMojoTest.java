package org.emergent.maven.gitver.plugin;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.VersionConfig;
import org.emergent.maven.gitver.core.version.RefVersionData;
import org.emergent.maven.gitver.core.version.SemVer;
import org.emergent.maven.gitver.core.version.VersionStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractGitverMojoTest {

  AbstractGitverMojo testMojo = new AbstractGitverMojo() {

    {
      mavenProject = new MavenProject();
      mavenProject.setFile(new File("my/pom.xml"));
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {}
  };

  @Test
  public void getVersionConfig() {
    assertThat(testMojo)
      .isNotNull()
      .extracting("disabled", "versionPattern", "versionOverride")
      .containsExactly(false, VersionConfig.DEFAULT_VERSION_PATTERN, "");
    assertThat(testMojo).isNotNull();
    assertThat(testMojo)
      .extracting("initialMajor", "initialMinor", "initialPatch")
      .containsExactly(0, 0, 0);
    assertThat(testMojo)
      .isNotNull()
      .extracting("majorKey", "minorKey", "patchKey", "useRegex")
      .containsExactly(VersionConfig.KEY_MAJOR, VersionConfig.KEY_MINOR, VersionConfig.KEY_PATCH, false);
  }

  @Test
  public void setVersionConfig() {
    AbstractGitverMojo mojo =
      new AbstractGitverMojo() {
        @Override
        public void execute() throws MojoExecutionException, MojoFailureException {}
      };
    mojo.setInitialMajor(1);
    assertThat(mojo)
      .isNotNull()
      .extracting("initialMajor", "initialMinor", "initialPatch")
      .containsExactly(1, 0, 0);
    assertThat(testMojo)
      .isNotNull()
      .extracting("majorKey", "minorKey", "patchKey", "useRegex")
      .containsExactly(VersionConfig.KEY_MAJOR, VersionConfig.KEY_MINOR, VersionConfig.KEY_PATCH, false);
  }

  @Test
  public void getVersionStrategy() {
    VersionStrategy versioner = testMojo.getVersionStrategy();
    assertThat(versioner).isNotNull().isInstanceOf(VersionStrategy.class);
  }

  @Test
  public void replaceVersionToken() {
    assertThat(testMojo.replaceTokens("v%v", new SemVerStrategy(1, 2, 3, "test", "testHash", VersionConfig.create())))
      .isEqualTo("v1.2.3");
  }

  @Getter
  private static class SemVerStrategy implements VersionStrategy {

    private final AtomicInteger commitCount = new AtomicInteger(0);
    private final String branch;
    @Setter
    private String hash;
    private final SemVer semVer;
    private final VersionConfig versionConfig;

    public SemVerStrategy(int major,
                          int minor,
                          int patch,
                          String branch,
                          String hash,
                          VersionConfig versionConfig) {
      this(branch, hash, SemVer.builder().setMajor(major).setMinor(minor).setPatch(patch).build(), versionConfig);
    }

    public SemVerStrategy(String branch, String hash, SemVer semVer, VersionConfig versionConfig) {
      this.branch = branch;
      this.hash = hash;
      this.semVer = semVer;
      this.versionConfig = versionConfig;
    }

    @Override
    public String toVersionString() {
      return semVer.toString();
    }

    public Map<String, String> toProperties() {
      Map<String, Object> properties = new TreeMap<>();
      properties.put("gitver.version", toVersionString());
      properties.putAll(versionConfig.toProperties());
      properties.putAll(getRefVersionData().toProperties());
      return Util.flatten(properties);
    }

    private RefVersionData getRefVersionData() {
      return RefVersionData.builder()
        .setBranch(branch)
        .setHash(hash)
        .setMajor(semVer.getMajor())
        .setMinor(semVer.getMinor())
        .setPatch(semVer.getPatch())
        .setCommit(commitCount.get())
        .build();
    }

    @Override
    public String toString() {
      return String.format("%s [branch: %s, version: %s, hash: %s]",
        getClass().getSimpleName(), branch, toVersionString(), hash);
    }
  }
}
