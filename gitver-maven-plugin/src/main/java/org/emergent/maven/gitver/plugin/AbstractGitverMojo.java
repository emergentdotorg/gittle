package org.emergent.maven.gitver.plugin;

import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.emergent.maven.gitver.core.Constants;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.git.GitUtil;
import org.emergent.maven.gitver.core.version.StrategyFactory;
import org.emergent.maven.gitver.core.version.VersionStrategy;

@Getter
@Setter
public abstract class AbstractGitverMojo extends org.apache.maven.plugin.AbstractMojo {

    private static final String DOTTED_PREFIX = Constants.GITTLE + ".";
    protected static final String NEW_VERSION_PROP = DOTTED_PREFIX + Constants.NEW_VERSION;
    protected static final String RELEASE_BRANCHES_PROP = DOTTED_PREFIX + Constants.RELEASE_BRANCHES;
    protected static final String VERSION_PATTERN_PROP = DOTTED_PREFIX + Constants.VERSION_PATTERN;
    protected static final String TAG_NAME_PATTERN_PROP = DOTTED_PREFIX + Constants.TAG_NAME_PATTERN;

    @Parameter(name = "skip", defaultValue = "false", property = "gittle.skip")
    protected boolean skip;
    
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject mavenProject;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            execute0();
        } catch (MojoExecutionException | MojoFailureException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new GitverException(e.getMessage(), e);
        }
    }

    protected void execute0() throws Exception {}

    protected GitUtil getGitUtil() {
        return GitUtil.getInstance(mavenProject.getBasedir());
    }

    public GitverConfig getConfig() {
        return Util.loadConfig(mavenProject.getBasedir().toPath());
    }

    protected VersionStrategy getVersionStrategy() {
        return StrategyFactory.getVersionStrategy(mavenProject.getBasedir(), getConfig());
    }

    protected String replaceTokens(String pattern, VersionStrategy versionStrategy) {
        return pattern.replace("%v", versionStrategy.toVersionString());
    }
}
