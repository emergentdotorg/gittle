package org.emergent.gittle.maven.plugin;

import static org.emergent.gittle.maven.plugin.util.PluginConfig.DELETE_TEMPORARY_FILE;
import static org.emergent.gittle.maven.plugin.util.PluginConfig.GENERATE_TEMPORARY_FILE;
import static org.emergent.gittle.maven.plugin.util.PluginConfig.PROPERTY_PREFIX;
import static org.emergent.gittle.maven.plugin.util.PluginConfig.UPDATE_DEPENDENCIES;

import lombok.Getter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.jspecify.annotations.NullUnmarked;

/**
 * External Version extension configuration Mojo.  This mojo is ONLY used to configure the extension.
 */
@Getter
@NullUnmarked
@Mojo(name = "verinf")
public class VersionInferenceMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject mavenProject;

    @Parameter(property = "strategy", defaultValue = "<strategy hint=\"git\"/>")
    private String strategy;

    @Parameter(property = PROPERTY_PREFIX + DELETE_TEMPORARY_FILE, defaultValue = "false")
    private Boolean deleteTemporaryFile;

    @Parameter(property = PROPERTY_PREFIX + GENERATE_TEMPORARY_FILE, defaultValue = "false")
    private Boolean generateTemporaryFile;

    @Parameter(property = PROPERTY_PREFIX + UPDATE_DEPENDENCIES, defaultValue = "false")
    private Boolean updateDependencies;

    @Override
    public void execute() {
        mavenProject.getBuildExtensions().stream().findFirst().ifPresent(ext -> {
            getLog().warn("Running version inference extension: " + ext);
        });
        getLog().info("This mojo is used to configure an extension, and should NOT be executed directly.");
    }
}
