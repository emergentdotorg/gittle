package org.emergent.maven.gitver.plugin;

import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.git.GitUtil;
import org.emergent.maven.gitver.core.version.StrategyFactory;
import org.emergent.maven.gitver.core.version.VersionStrategy;

@Getter
@Setter
public abstract class AbstractGitverMojo extends org.apache.maven.plugin.AbstractMojo {

  @Parameter(name = "skip", defaultValue = "false", property = "gv.skip")
  protected boolean skip;

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  protected MavenProject mavenProject;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    execute0();
  }

  protected void execute0() throws MojoExecutionException, MojoFailureException {
  }

  protected GitUtil getGitUtil() {
    return GitUtil.getInstance(mavenProject.getBasedir());
  }

  public GitverConfig getConfig() {
    return Util.loadConfig(mavenProject.getBasedir().toPath());
  }

  protected VersionStrategy getVersionStrategy() {
    return StrategyFactory.getVersionStrategy(mavenProject.getBasedir(), getConfig());
  }

  protected String replaceTokens(String pattern, VersionStrategy versionStrategy) {
    return pattern.replace("%v", versionStrategy.toVersionString());
  }
}
