package org.emergent.maven.gitver.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.emergent.maven.gitver.core.version.OverrideStrategy;
import org.emergent.maven.gitver.core.version.VersionStrategy;
import org.junit.jupiter.api.Test;

public class AbstractGitverMojoTest {

    AbstractGitverMojo testMojo = new AbstractGitverMojo() {

        {
            mavenProject = new MavenProject();
            mavenProject.setFile(new File("my/pom.xml"));
        }

        @Override
        public void execute() throws MojoExecutionException, MojoFailureException {}
    };

    @Test
    public void getVersionConfig() {
        assertThat(testMojo).isNotNull().extracting("skip").isEqualTo(false);
        assertThat(testMojo).isNotNull();
    }

    @Test
    public void getVersionStrategy() {
        VersionStrategy strategy = testMojo.getVersionStrategy();
        assertThat(strategy).isNotNull().isInstanceOf(VersionStrategy.class);
    }

    @Test
    public void replaceVersionToken() {
        assertThat(testMojo.replaceTokens("v%v", new OverrideStrategy("1.2.3"))).isEqualTo("v1.2.3");
    }
}
