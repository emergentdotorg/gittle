package org.emergent.maven.gitver.plugin;

import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.git.GitUtil;
import org.emergent.maven.gitver.core.version.VersionStrategy;

@Getter
@Setter
@Mojo(name = "tag")
public class TagMojo extends AbstractGitverMojo {

  @Parameter(name = "failWhenTagExist", defaultValue = "true", property = "tag.failWhenTagExist")
  private boolean failWhenTagExist = true;

  @Parameter(name = "tagMessagePattern", defaultValue = "Release version %v", property = "tag.messagePattern")
  private String tagMessagePattern = "Release version %v";

  @Parameter(name = "tagNamePattern", defaultValue = "v%v", property = "tag.namePattern")
  private String tagNamePattern = "v%v";

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    VersionStrategy versionStrategy = getVersionStrategy();
    String tagName = replaceTokens(getTagNamePattern(), versionStrategy);
    String tagMessage = replaceTokens(getTagMessagePattern(), versionStrategy);
    getLog().info("Current Version: " + versionStrategy.toVersionString());
    getLog().info(String.format("Tag Version '%s' with message '%s'", tagName, tagMessage));
    GitUtil gitUtil = GitUtil.getInstance();
    if (gitUtil.tagExists(mavenProject.getBasedir().getAbsoluteFile(), tagName)) {
      getLog().error(String.format("Tag already exist: %s", tagName));
      if (isFailWhenTagExist()) throw new GitverException("Tag already exist: " + tagName);
    } else {
      String tagId = gitUtil.createTag(mavenProject.getBasedir().getAbsoluteFile(), tagName, tagMessage);
      getLog().info(String.format("Created tag: '%s'", tagId));
    }
  }
}
