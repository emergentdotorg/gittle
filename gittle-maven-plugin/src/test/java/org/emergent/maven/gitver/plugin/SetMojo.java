package org.emergent.maven.gitver.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.emergent.maven.gitver.core.Util;

import java.nio.file.Files;
import java.nio.file.Path;

@Mojo(name = "set", defaultPhase = LifecyclePhase.INITIALIZE)
public class SetMojo extends AbstractGitverMojo {

    @Override
    protected void execute0() throws MojoExecutionException, MojoFailureException {
        Path basedir = mavenProject.getBasedir().toPath();
        // Path targetdir = basedir.resolve(mavenProject.getBuild().getDirectory());
        Path newPom = basedir.resolve(Util.GITVER_POM_XML);
        if (Files.exists(newPom)) {
            mavenProject.setPomFile(newPom.toFile());
        } else {
            throw new MojoExecutionException("Cannot find " + newPom);
        }
    }
}
