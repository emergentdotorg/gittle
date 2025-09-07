package org.emergent.maven.gitver.plugin;

import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;

@Mojo(name = "print", defaultPhase = LifecyclePhase.VALIDATE)
public class PrintMojo extends AbstractGitverMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    Map<String, String> properties = getGitverProperties(getVersionStrategy());
    MessageBuilder builder = MessageUtils.buffer().a("properties:");
    properties.forEach((k,v) -> builder.newline().format("	%s=%s", k, v));
    getLog().info("Gitver " + builder);
  }
}
