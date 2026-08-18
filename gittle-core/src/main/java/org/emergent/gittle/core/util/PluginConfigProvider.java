package org.emergent.gittle.core.util;

import java.util.HashMap;
import java.util.Map;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;

public class PluginConfigProvider {

    private final Map<MavenProject, PluginConfig> projectConfigs;
    private final PlexusContainer container;

    public PluginConfigProvider(PlexusContainer container) {
        this(new HashMap<>(), container);
    }

    public PluginConfigProvider(Map<MavenProject, PluginConfig> projectConfigs, PlexusContainer container) {
        this.projectConfigs = projectConfigs;
        this.container = container;
    }

    public PluginConfig getForProject(MavenProject mavenProject) throws MavenExecutionException {
        // Can't use computeIfAbsent here elegantly due to checked exceptions
        if (projectConfigs.containsKey(mavenProject)) {
            return projectConfigs.get(mavenProject);
        }
        PluginConfig pluginConfig = PluginConfig.of(mavenProject, container);
        projectConfigs.put(mavenProject, pluginConfig);
        return pluginConfig;
    }
}
