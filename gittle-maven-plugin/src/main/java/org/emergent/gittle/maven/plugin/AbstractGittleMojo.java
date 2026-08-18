package org.emergent.gittle.maven.plugin;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.name;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

import javax.inject.Inject;
import lombok.Getter;
import lombok.Setter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.emergent.gittle.maven.plugin.git.GitUtil;
import org.emergent.gittle.maven.plugin.util.ModelProvider;
import org.emergent.gittle.maven.plugin.util.PluginConfigProvider;

@Getter
@Setter
public abstract class AbstractGittleMojo extends org.apache.maven.plugin.AbstractMojo {

    private static final String DOTTED_PREFIX = Constants.GITTLE + ".";
    protected static final String NEW_VERSION_PROP = DOTTED_PREFIX + Constants.NEW_VERSION;
    protected static final String RELEASE_BRANCHES_PROP = DOTTED_PREFIX + Constants.RELEASE_BRANCHES;
    protected static final String VERSION_PATTERN_PROP = DOTTED_PREFIX + Constants.VERSION_PATTERN;
    protected static final String TAG_NAME_PATTERN_PROP = DOTTED_PREFIX + Constants.TAG_NAME_PATTERN;

    @Parameter(name = "skip", defaultValue = "false", property = "gittle.skip")
    protected boolean skip;

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    protected MavenProject mavenProject;

    @Parameter(defaultValue = "${session}", required = true, readonly = true)
    protected MavenSession mavenSession;

    @Parameter(defaultValue = "${pluginManager}", required = true, readonly = true)
    private BuildPluginManager pluginManager;

    @Parameter(property = "strategy", defaultValue = "<strategy hint=\"git\"/>", required = true)
    private String strategy;

    @Inject
    private PluginConfigProvider pluginConfigProvider;

    @Inject
    private ModelProvider modelProvider;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            execute0();
        } catch (MojoExecutionException | MojoFailureException | Error e) {
            throw e;
        } catch (Exception e) {
            throw new GittleException(e.getMessage(), e);
        }
    }

    protected void execute0() throws Exception {
    }

    protected GitUtil getGitUtil() {
        return GitUtil.getInstance(mavenProject.getBasedir());
    }

    // public Config getConfig() {
    //   Properties extensionProps = Util.loadProperties(mavenProject.getBasedir().toPath());
    //   return Config.builder().build();
    // }

    // protected String replaceTokens(String pattern, VerStrategy versionStrategy) {
    //   return pattern.replace("%v", versionStrategy.version());
    // }

    protected void invokeMojo() throws MojoExecutionException {
        executeMojo(
                plugin(
                        groupId("org.codehaus.mojo"),
                        artifactId("flatten-maven-plugin"),
                        version("1.7.3")
                ),
                goal("flatten"),
                configuration(
                        element(name("updatePomFile"), "true"),
                        element(
                                name("pomElements"),
                                element(name("parent"), "keep")
                        ),
                        element(name("flattenMode"), "resolveCiFriendliesOnly")
                ),
                executionEnvironment(mavenProject, mavenSession, pluginManager)
        );
    }
}
