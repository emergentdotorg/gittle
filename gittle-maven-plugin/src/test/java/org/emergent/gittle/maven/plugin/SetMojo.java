package org.emergent.gittle.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

import java.nio.file.Files;
import java.nio.file.Path;

@Mojo(name = "set", defaultPhase = LifecyclePhase.INITIALIZE)
public class SetMojo extends AbstractGittleMojo {

  @Override
  protected void execute0() throws MojoExecutionException, MojoFailureException {
    Path basedir = mavenProject.getBasedir().toPath();
    // Path targetdir = basedir.resolve(mavenProject.getBuild().getDirectory());
    Path newPom = basedir.resolve(Util.GITTLE_POM_XML);
    if (Files.exists(newPom)) {
      mavenProject.setPomFile(newPom.toFile());
    } else {
      throw new MojoExecutionException("Cannot find " + newPom);
    }
  }
}
