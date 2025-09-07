package org.emergent.maven.gitver.plugin;

import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "set", defaultPhase = LifecyclePhase.INITIALIZE)
public class SetMojo extends AbstractGitverMojo {

  @Override
  public void execute() throws MojoExecutionException {
    Path newPom = Util.getNewPom(mavenProject);
    if (Files.exists(newPom)) {
      mavenProject.setPomFile(newPom.toFile());
    } else {
      throw new MojoExecutionException("Cannot find " + newPom);
    }
  }
}
