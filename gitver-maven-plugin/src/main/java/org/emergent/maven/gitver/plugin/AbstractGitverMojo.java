package org.emergent.maven.gitver.plugin;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.emergent.maven.gitver.core.VersionConfig;
import org.emergent.maven.gitver.core.git.GitUtil;
import org.emergent.maven.gitver.core.version.VersionStrategy;

import static org.emergent.maven.gitver.core.VersionConfig.GV_DISABLED;
import static org.emergent.maven.gitver.core.VersionConfig.GV_INITIAL_MAJOR;
import static org.emergent.maven.gitver.core.VersionConfig.GV_INITIAL_MINOR;
import static org.emergent.maven.gitver.core.VersionConfig.GV_INITIAL_PATCH;
import static org.emergent.maven.gitver.core.VersionConfig.GV_KEYWORDS_MAJOR;
import static org.emergent.maven.gitver.core.VersionConfig.GV_KEYWORDS_MINOR;
import static org.emergent.maven.gitver.core.VersionConfig.GV_KEYWORDS_PATCH;
import static org.emergent.maven.gitver.core.VersionConfig.GV_KEYWORDS_REGEX;
import static org.emergent.maven.gitver.core.VersionConfig.GV_VERSION_OVERRIDE;
import static org.emergent.maven.gitver.core.VersionConfig.GV_VERSION_PATTERN;
import static org.emergent.maven.gitver.core.VersionConfig.KEY_MAJOR;
import static org.emergent.maven.gitver.core.VersionConfig.KEY_MINOR;
import static org.emergent.maven.gitver.core.VersionConfig.KEY_PATCH;

@Getter
@Setter
public abstract class AbstractGitverMojo extends org.apache.maven.plugin.AbstractMojo {

  @Parameter(name = "disabled", defaultValue = "false", property = GV_DISABLED)
  protected boolean disabled = false;

  @Parameter(name = "initialMajor", defaultValue = "0", property = GV_INITIAL_MAJOR)
  protected int initialMajor = 0;

  @Parameter(name = "initialMinor", defaultValue = "0", property = GV_INITIAL_MINOR)
  protected int initialMinor = 0;

  @Parameter(name = "initialPatch", defaultValue = "0", property = GV_INITIAL_PATCH)
  protected int initialPatch = 0;

  @Parameter(name = "majorKey", defaultValue = KEY_MAJOR, property = GV_KEYWORDS_MAJOR)
  protected String majorKey = KEY_MAJOR;

  @Parameter(name = "minorKey", defaultValue = KEY_MINOR, property = GV_KEYWORDS_MINOR)
  protected String minorKey = KEY_MINOR;

  @Parameter(name = "patchKey", defaultValue = KEY_PATCH, property = GV_KEYWORDS_PATCH)
  protected String patchKey = KEY_PATCH;

  @Parameter(name = "useRegex", defaultValue = "false", property = GV_KEYWORDS_REGEX)
  protected boolean useRegex = false;

  @Parameter(name = "versionPattern", defaultValue = VersionConfig.DEFAULT_VERSION_PATTERN, property = GV_VERSION_PATTERN)
  protected String versionPattern = VersionConfig.DEFAULT_VERSION_PATTERN;

  @Parameter(name = "versionOverride", defaultValue = "", property = GV_VERSION_OVERRIDE)
  protected String versionOverride = "";

  @Parameter(defaultValue = "${project}", required = true, readonly = true)
  protected MavenProject mavenProject;

  public VersionConfig toVersionConfig() {
    return VersionConfig.builder()
      .setDisabled(disabled)
      .setInitialMajor(initialMajor)
      .setInitialMinor(initialMinor)
      .setInitialPatch(initialPatch)
      .setMajorKey(majorKey)
      .setMinorKey(minorKey)
      .setPatchKey(patchKey)
      .setUseRegex(useRegex)
      .setVersionPattern(versionPattern)
      .setVersionOverride(versionOverride)
      .build();
  }

  protected Map<String, String> getGitverProperties(VersionStrategy versionStrategy) {
    return versionStrategy.toProperties();
  }

  protected VersionStrategy getVersionStrategy() {
    return GitUtil.getVersionStrategy(mavenProject.getBasedir(), toVersionConfig());
  }

  protected String replaceTokens(String pattern, VersionStrategy versionStrategy) {
    return pattern.replace("%v", versionStrategy.toVersionString());
  }
}
