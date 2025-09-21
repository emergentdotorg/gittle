package org.emergent.maven.gitver.plugin;

import java.util.Objects;
import lombok.Setter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.emergent.maven.gitver.core.GitverException;

public class CommitMojo extends AbstractGitverMojo {

    @Setter
    @Parameter(name = "message", property = "gittle.commitMessage", defaultValue = "Empty Commit")
    private String message;

    @Setter
    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    private MavenSession mavenSession;

    @Override
    protected void execute0() throws Exception {
        if (!Objects.equals(mavenSession.getTopLevelProject(), mavenProject)) {
            getLog().debug("Skipping CommitMojo in child module: " + mavenProject.getArtifactId());
            return;
        }
        try {
            getGitUtil().executeCommit(message);
        } catch (GitverException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }
}
