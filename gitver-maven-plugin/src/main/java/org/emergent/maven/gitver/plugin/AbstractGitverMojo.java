package org.emergent.maven.gitver.plugin;

import java.util.Map;
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

//  @Parameter(name = "disabled", defaultValue = "false", property = GV_DISABLED)
//  protected boolean disabled = false;
//
//  @Parameter(name = "initialMajor", defaultValue = "0", property = GV_INITIAL_MAJOR)
//  protected int initialMajor = 0;
//
//  @Parameter(name = "initialMinor", defaultValue = "0", property = GV_INITIAL_MINOR)
//  protected int initialMinor = 0;
//
//  @Parameter(name = "initialPatch", defaultValue = "0", property = GV_INITIAL_PATCH)
//  protected int initialPatch = 0;
//
//  @Parameter(name = "majorKey", defaultValue = KEY_MAJOR, property = GV_KEYWORDS_MAJOR)
//  protected String majorKey = KEY_MAJOR;
//
//  @Parameter(name = "minorKey", defaultValue = KEY_MINOR, property = GV_KEYWORDS_MINOR)
//  protected String minorKey = KEY_MINOR;
//
//  @Parameter(name = "patchKey", defaultValue = KEY_PATCH, property = GV_KEYWORDS_PATCH)
//  protected String patchKey = KEY_PATCH;
//
//  @Parameter(name = "useRegex", defaultValue = "false", property = GV_KEYWORDS_REGEX)
//  protected boolean useRegex = false;
//
//  @Parameter(name = "versionPattern", defaultValue = VersionConfig.DEFAULT_VERSION_PATTERN, property = GV_VERSION_PATTERN)
//  protected String versionPattern = VersionConfig.DEFAULT_VERSION_PATTERN;
//
//  @Parameter(name = "versionOverride", defaultValue = "", property = GV_VERSION_OVERRIDE)
//  protected String versionOverride = "";

  @Parameter(name = "skip", defaultValue = "false", property = "gv.skip")
  protected boolean skip;

//  @Parameter(name = "initialMajor", property = GV_INITIAL_MAJOR)
//  protected int initialMajor;
//
//  @Parameter(name = "initialMinor", property = GV_INITIAL_MINOR)
//  protected int initialMinor;
//
//  @Parameter(name = "initialPatch", property = GV_INITIAL_PATCH)
//  protected int initialPatch;
//
//  @Parameter(name = "useRegex", property = GV_KEYWORDS_REGEX)
//  protected Boolean useRegex;
//
//  @Parameter(name = "versionPattern", property = GV_VERSION_PATTERN)
//  protected String versionPattern;
//
//  @Parameter(name = "versionOverride", property = GV_VERSION_OVERRIDE)
//  protected String versionOverride;

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

  protected Map<String, String> getGitverProperties(VersionStrategy versionStrategy) {
    return versionStrategy.toProperties();
  }

  protected VersionStrategy getVersionStrategy() {
    StrategyFactory factory = StrategyFactory.getInstance(mavenProject.getBasedir());
    return factory.getVersionStrategy(getConfig());
  }

  protected String replaceTokens(String pattern, VersionStrategy versionStrategy) {
    return pattern.replace("%v", versionStrategy.toVersionString());
  }
}
