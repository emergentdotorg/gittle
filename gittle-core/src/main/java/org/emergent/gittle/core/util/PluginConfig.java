package org.emergent.gittle.core.util;

import org.emergent.gittle.core.strategy.VersionStrategy;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.expression.DefaultExpressionEvaluator;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import java.io.File;
import java.util.Optional;

public class PluginConfig {

    public static final String PROPERTY_PREFIX = "version-inference";
    public static final String GENERATE_TEMPORARY_FILE = "generateTemporaryFile";
    public static final String DELETE_TEMPORARY_FILE = "deleteTemporaryFile";
    public static final String UPDATE_DEPENDENCIES = "updateDependencies";
    public static final String FULL_PLUGIN_NAME = "org.emergent.maven.plugins:gittle-maven-plugin";
    public static final String STRATEGY_NODE_NAME = "strategy";
    public static final String STRATEGY_HINT = "hint";

    public final boolean shouldGenerateTemporaryFile;
    public final boolean shouldDeleteTemporaryFile;
    public final boolean shouldUpdateDependencies;
    public final VersionStrategy versionStrategy;

    public PluginConfig(
            boolean shouldGenerateTemporaryFile,
            boolean shouldDeleteTemporaryFile,
            boolean shouldUpdateDependencies,
            VersionStrategy versionStrategy
    ) {
        this.shouldGenerateTemporaryFile = shouldGenerateTemporaryFile;
        this.shouldDeleteTemporaryFile = shouldDeleteTemporaryFile;
        this.shouldUpdateDependencies = shouldUpdateDependencies;
        this.versionStrategy = versionStrategy;
    }

    public static PluginConfig of(MavenProject mavenProject, PlexusContainer container)
            throws MavenExecutionException {
        // Lookup this plugin's configuration from the project
        Plugin plugin = mavenProject.getPlugin(FULL_PLUGIN_NAME);
        if (plugin != null) {
            Xpp3Dom pluginConfigDom = (Xpp3Dom) plugin.getConfiguration();
            return new PluginConfig(
                getBooleanConfigValue(pluginConfigDom, GENERATE_TEMPORARY_FILE),
                getBooleanConfigValue(pluginConfigDom, DELETE_TEMPORARY_FILE),
                getBooleanConfigValue(pluginConfigDom, UPDATE_DEPENDENCIES),
                getStrategy(pluginConfigDom, mavenProject.getFile(), container)
            );
        }
        return null;
    }

    private static boolean getBooleanConfigValue(Xpp3Dom pluginConfigDom, String nodeName) {
        Xpp3Dom n = pluginConfigDom.getChild(nodeName);
        return n != null && Boolean.parseBoolean(n.getValue());
    }

    private static VersionStrategy getStrategy(Xpp3Dom configDom, File pomFile, PlexusContainer container)
            throws MavenExecutionException
    {
        // Get the requested strategy from the POM config
        Xpp3Dom strategyNode = Optional.ofNullable(configDom.getChild(STRATEGY_NODE_NAME))
                .orElseThrow(() -> new MavenExecutionException(
                        "Missing configuration, " + STRATEGY_NODE_NAME + " is required. ",
                        pomFile
                ));

        String hint = Optional.ofNullable(strategyNode.getAttribute(STRATEGY_HINT))
                .orElseThrow(() -> new MavenExecutionException(
                        "Missing config; " + STRATEGY_NODE_NAME + " " + STRATEGY_HINT + " attribute is required.",
                        pomFile
                ));

        try {
            // Get and configure the strategy
            VersionStrategy strategy = container.lookup(VersionStrategy.class, hint);
            ComponentConfigurator configurator = container.lookup(ComponentConfigurator.class, "basic");
            configurator.configureComponent(
                    strategy,
                    new XmlPlexusConfiguration(strategyNode),
                    new DefaultExpressionEvaluator(),
                    null,
                    null
            );
            return strategy;
        } catch (ComponentLookupException | ComponentConfigurationException e) {
            throw new MavenExecutionException(e.getMessage(), e);
        }
    }
}
