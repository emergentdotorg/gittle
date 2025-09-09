package org.emergent.maven.gitver.plugin;

import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.emergent.maven.gitver.core.GitVerConfig;
import org.emergent.maven.gitver.core.version.VersionStrategy;

import static org.emergent.maven.gitver.core.GitVerConfig.GV_DISABLED;
import static org.emergent.maven.gitver.core.GitVerConfig.GV_VERSION_OVERRIDE;
import static org.emergent.maven.gitver.core.GitVerConfig.GV_VERSION_PATTERN;

@Getter
@Setter
public abstract class AbstractGitverMojo extends org.apache.maven.plugin.AbstractMojo {

  @Parameter(name = "disabled", defaultValue = "false", property = GV_DISABLED)
  protected boolean disabled = false;

//  @Parameter(name = "initialMajor", defaultValue = "0", property = GV_INITIAL_MAJOR)
//  protected int initialMajor = 0;
//
//  @Parameter(name = "initialMinor", defaultValue = "0", property = GV_INITIAL_MINOR)
//  protected int initialMinor = 0;
//
//  @Parameter(name = "initialPatch", defaultValue = "0", property = GV_INITIAL_PATCH)
//  protected int initialPatch = 0;

  @Parameter(name = "versionPattern", defaultValue = GitVerConfig.DEFAULT_VERSION_PATTERN, property = GV_VERSION_PATTERN)
  protected String versionPattern = GitVerConfig.DEFAULT_VERSION_PATTERN;

  @Parameter(name = "versionOverride", defaultValue = "", property = GV_VERSION_OVERRIDE)
  protected String versionOverride = "";

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  protected MavenProject mavenProject;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (disabled) {
      return;
    }
    execute0();
  }

  public abstract void execute0() throws MojoExecutionException, MojoFailureException;

  protected VersionStrategy getVersionStrategy() {
    return Util.getVersionStrategy(mavenProject);
  }
}
