package org.emergent.maven.gitver.plugin;

import java.util.Objects;
import lombok.Setter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.git.GitUtil;

public abstract class CommitMojo extends AbstractGitverMojo {

  private static final String KEYWORD_TOKEN = "[%k]";

  @Setter
  @Parameter(name = "message", property = "gv.commitMessage", defaultValue = "chore(release): " + KEYWORD_TOKEN)
  private String message;

  @Setter
  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession mavenSession;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!Objects.equals(mavenSession.getTopLevelProject(), mavenProject)) {
      getLog().debug("Skipping CommitMojo in child module: " + mavenProject.getArtifactId());
      return;
    }
    String msg = message;
    if (!msg.contains(KEYWORD_TOKEN)) {
      msg = msg + " " + KEYWORD_TOKEN;
    }
    String resolvedMessage = msg.replace(KEYWORD_TOKEN, getKeyword());
    try {
      GitUtil.getInstance().executeCommit(mavenProject.getBasedir(), resolvedMessage);
    } catch (GitverException e) {
      throw new MojoFailureException(e.getMessage(), e);
    }
  }

  protected abstract String getKeyword();

  @Mojo(name = "commit-major")
  public static class VersionCommitMajorMojo extends CommitMojo {
    @Override
    protected String getKeyword() {
      return getMajorKey();
    }
  }

  @Mojo(name = "commit-minor")
  public static class VersionCommitMinorMojo extends CommitMojo {
    @Override
    protected String getKeyword() {
      return getMinorKey();
    }
  }

  @Mojo(name = "commit-patch")
  public static class VersionCommitPatchMojo extends CommitMojo {
    @Override
    protected String getKeyword() {
      return getPatchKey();
    }
  }
}
