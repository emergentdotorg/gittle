package org.emergent.maven.gitver.plugin;

import lombok.Getter;
import lombok.Setter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.git.GitUtil;

import java.util.Objects;

import static org.emergent.maven.gitver.core.GitVerConfig.GV_KEYWORDS_MAJOR;
import static org.emergent.maven.gitver.core.GitVerConfig.GV_KEYWORDS_MINOR;
import static org.emergent.maven.gitver.core.GitVerConfig.GV_KEYWORDS_PATCH;
import static org.emergent.maven.gitver.core.GitVerConfig.GV_KEYWORDS_REGEX;
import static org.emergent.maven.gitver.core.GitVerConfig.KEY_MAJOR;
import static org.emergent.maven.gitver.core.GitVerConfig.KEY_MINOR;
import static org.emergent.maven.gitver.core.GitVerConfig.KEY_PATCH;

@Getter
public abstract class CommitMojo extends AbstractGitverMojo {

  public static final String KEYWORD_TOKEN = "[%k]";

  public enum IncrementType {
    MAJOR,
    MINOR,
    PATCH,
  }
  @Setter
  @Parameter(name = "message", property = "gitver.commit.message", defaultValue = "chore(release): " + KEYWORD_TOKEN)
  private String message;

  @Parameter(name = "majorKey", defaultValue = KEY_MAJOR, property = GV_KEYWORDS_MAJOR)
  protected String majorKey = KEY_MAJOR;

  @Parameter(name = "minorKey", defaultValue = KEY_MINOR, property = GV_KEYWORDS_MINOR)
  protected String minorKey = KEY_MINOR;

  @Parameter(name = "patchKey", defaultValue = KEY_PATCH, property = GV_KEYWORDS_PATCH)
  protected String patchKey = KEY_PATCH;

  @Parameter(name = "useRegex", defaultValue = "false", property = GV_KEYWORDS_REGEX)
  protected boolean useRegex = false;

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  protected MavenSession mavenSession;

  @Override
  public void execute0() throws MojoExecutionException, MojoFailureException {
    if (!Objects.equals(mavenSession.getTopLevelProject(), mavenProject)) {
      getLog().debug("Skipping CommitMojo in child module: " + mavenProject.getArtifactId());
      return;
    }
    if (!message.contains(KEYWORD_TOKEN)) message = message.concat(" " + KEYWORD_TOKEN);
    String resolvedMessage = message.replace(KEYWORD_TOKEN, getKeyword());
    try {
      boolean completed = GitUtil.executeCommit(mavenProject.getBasedir(), resolvedMessage);
      if (!completed) {
        throw new MojoFailureException("Timed out for creating commit");
      }
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
