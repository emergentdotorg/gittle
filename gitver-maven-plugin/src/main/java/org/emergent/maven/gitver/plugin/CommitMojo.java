package org.emergent.maven.gitver.plugin;

import java.util.Objects;
import java.util.Optional;
import lombok.Setter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.git.GitUtil;

import static org.emergent.maven.gitver.core.Constants.GV_KEYWORDS_MAJOR;
import static org.emergent.maven.gitver.core.Constants.GV_KEYWORDS_MINOR;
import static org.emergent.maven.gitver.core.Constants.GV_KEYWORDS_PATCH;


public abstract class CommitMojo extends AbstractGitverMojo {

  private static final String KEYWORD_TOKEN = "[%k]";

  @Setter
  @Parameter(name = "message", property = "gv.commitMessage", defaultValue = "chore(release): " + KEYWORD_TOKEN)
  private String message;

  @Setter
  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession mavenSession;

  protected void executeIt(String keyword) throws MojoExecutionException, MojoFailureException {
    if (!Objects.equals(mavenSession.getTopLevelProject(), mavenProject)) {
      getLog().debug("Skipping CommitMojo in child module: " + mavenProject.getArtifactId());
      return;
    }
    String msg = message;
    if (!msg.contains(KEYWORD_TOKEN)) {
      msg = msg + " " + KEYWORD_TOKEN;
    }
    String resolvedMessage = msg.replace(KEYWORD_TOKEN, keyword);
    try {
      GitUtil.getInstance(mavenProject.getBasedir()).executeCommit(resolvedMessage);
    } catch (GitverException e) {
      throw new MojoFailureException(e.getMessage(), e);
    }
  }

  @Mojo(name = "commit-major")
  public static class VersionCommitMajorMojo extends CommitMojo {

    @Setter
    @Parameter(name = "keyword", property = GV_KEYWORDS_MAJOR)
    private String keyword;

    @Override
    protected void execute0() throws MojoExecutionException, MojoFailureException {
      executeIt(Optional.ofNullable(keyword).orElse(toVersionConfig().getMajorKeywordsList().get(0)));
    }
  }

  @Mojo(name = "commit-minor")
  public static class VersionCommitMinorMojo extends CommitMojo {

    @Setter
    @Parameter(name = "keyword", property = GV_KEYWORDS_MINOR)
    private String keyword;

    @Override
    protected void execute0() throws MojoExecutionException, MojoFailureException {
      executeIt(Optional.ofNullable(keyword).orElse(toVersionConfig().getMinorKeywordsList().get(0)));
    }
  }

  @Mojo(name = "commit-patch")
  public static class VersionCommitPatchMojo extends CommitMojo {

    @Setter
    @Parameter(name = "keyword", property = GV_KEYWORDS_PATCH)
    private String keyword;

    @Override
    protected void execute0() throws MojoExecutionException, MojoFailureException {
      executeIt(Optional.ofNullable(keyword).orElse(toVersionConfig().getPatchKeywordsList().get(0)));
    }
  }
}
