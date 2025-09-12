package org.emergent.maven.gitver.plugin;

import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;

@Mojo(name = "props", defaultPhase = LifecyclePhase.INITIALIZE)
public class PropsMojo extends AbstractGitverMojo {

  @Override
  protected void execute0() throws MojoExecutionException, MojoFailureException {
    Map<String, String> properties = getGitverProperties(getVersionStrategy());
    MessageBuilder builder = MessageUtils.buffer().a("properties:");
    properties.forEach((k,v) -> builder.newline().format("	%s=%s", k, v));
    getLog().info("Adding generated properties to project model: " + builder);
    mavenProject.getProperties().putAll(properties);
  }

}
