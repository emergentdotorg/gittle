package org.emergent.gittle.maven.plugin.util;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;

@Singleton
public class PluginConfigProvider {

    private final Map<MavenProject, PluginConfig> configs;

    private final PlexusContainer container;

    @Inject
    public PluginConfigProvider(PlexusContainer container) {
        this(new HashMap<>(), container);
    }

    public PluginConfigProvider(Map<MavenProject, PluginConfig> configs, PlexusContainer container) {
        this.configs = configs;
        this.container = container;
    }

    public PluginConfig getForProject(MavenProject mavenProject) throws MavenExecutionException {
        // Can't use computeIfAbsent here elegantly due to checked exceptions
        if (configs.containsKey(mavenProject)) {
            return configs.get(mavenProject);
        }
        PluginConfig pluginConfig = PluginConfig.of(mavenProject, container);
        configs.put(mavenProject, pluginConfig);
        return pluginConfig;
    }
}
