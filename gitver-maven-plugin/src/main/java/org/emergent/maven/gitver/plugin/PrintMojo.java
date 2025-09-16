package org.emergent.maven.gitver.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.emergent.maven.gitver.core.Coordinates;
import org.emergent.maven.gitver.core.Util;

@Mojo(name = "print", defaultPhase = LifecyclePhase.VALIDATE)
public class PrintMojo extends AbstractGitverMojo {

    @Override
    protected void execute0() throws MojoExecutionException, MojoFailureException {
        getLog().info("Printing properties of project "
                + MessageUtils.buffer()
                        .mojo(Coordinates.builder()
                                .setGroupId(mavenProject.getGroupId())
                                .setArtifactId(mavenProject.getArtifactId())
                                .setVersion(mavenProject.getVersion())
                                .build())
                        .a(Util.join(getVersionStrategy().getPropertiesMap())));
    }
}
