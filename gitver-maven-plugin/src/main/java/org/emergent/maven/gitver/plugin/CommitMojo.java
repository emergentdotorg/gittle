package org.emergent.maven.gitver.plugin;

import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.git.GitUtil;

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

  @Getter
  private final IncrementType incrementType;

  public CommitMojo(IncrementType incrementType) {
    this.incrementType = incrementType;
  }

  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  protected MavenSession mavenSession;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (!Objects.equals(mavenSession.getTopLevelProject(), mavenProject)) {
      getLog().debug("Skipping CommitMojo in child module: " + mavenProject.getArtifactId());
      return;
    }
    String typeName = switch (getIncrementType()) {
      case MAJOR -> getMajorKey();
      case MINOR -> getMinorKey();
      case PATCH -> getPatchKey();
    };
    if (!message.contains(KEYWORD_TOKEN)) message = message.concat(" " + KEYWORD_TOKEN);
    String resolvedMessage = message.replace(KEYWORD_TOKEN, typeName);
    try {
      boolean completed = GitUtil.executeCommit(mavenProject.getBasedir(), resolvedMessage);
      if (!completed) {
        throw new MojoFailureException("Timed out for creating commit");
      }
    } catch (GitverException e) {
      throw new MojoFailureException(e.getMessage(), e);
    }
  }

  @Mojo(name = "commit-major")
  public static class VersionCommitMajorMojo extends CommitMojo {
    public VersionCommitMajorMojo() {
      super(IncrementType.MAJOR);
    }
  }

  @Mojo(name = "commit-minor")
  public static class VersionCommitMinorMojo extends CommitMojo {
    public VersionCommitMinorMojo() {
      super(IncrementType.MINOR);
    }
  }

  @Mojo(name = "commit-patch")
  public static class VersionCommitPatchMojo extends CommitMojo {
    public VersionCommitPatchMojo() {
      super(IncrementType.PATCH);
    }
  }
}
