package org.emergent.gittle.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.emergent.gittle.BaseTestCase;
import org.emergent.gittle.core.strategy.VersionStrategy;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

public class PluginConfigProviderTest extends BaseTestCase {

    @Mock
    private PlexusContainer container;

    private Map<MavenProject, PluginConfig> projectConfigs;

    private PluginConfigProvider provider;

    @Before
    public void setUp() {
        projectConfigs = new HashMap<>();
        provider = new PluginConfigProvider(projectConfigs, container);
    }

    @Test
    public void getForProject_AlreadyExists() throws MavenExecutionException {
        final MavenProject project = new MavenProject();
        final PluginConfig existingConfig = new PluginConfig(false, false, false, mock(VersionStrategy.class));
        projectConfigs.put(project, existingConfig);
        final PluginConfig config = provider.getForProject(project);
        assertThat(config).isEqualTo(existingConfig);
    }

    @Test
    public void getForProject() throws Exception {
        final MavenProject project = mock(MavenProject.class);
        final Plugin plugin = mock(Plugin.class);
        when(project.getPlugin(PluginConfig.FULL_PLUGIN_NAME)).thenReturn(plugin);
        final Xpp3Dom configDom = mock(Xpp3Dom.class);
        when(plugin.getConfiguration()).thenReturn(configDom);
        final Xpp3Dom genTempFileDom = mock(Xpp3Dom.class);
        final Xpp3Dom deleteTempFileDom = mock(Xpp3Dom.class);
        final Xpp3Dom updateDepMgmtDom = mock(Xpp3Dom.class);
        when(configDom.getChild(PluginConfig.GENERATE_TEMPORARY_FILE)).thenReturn(genTempFileDom);
        when(configDom.getChild(PluginConfig.DELETE_TEMPORARY_FILE)).thenReturn(deleteTempFileDom);
        when(configDom.getChild(PluginConfig.UPDATE_DEPENDENCIES)).thenReturn(updateDepMgmtDom);
        when(genTempFileDom.getValue()).thenReturn("false");
        when(deleteTempFileDom.getValue()).thenReturn("true");
        when(updateDepMgmtDom.getValue()).thenReturn("false");
        final Xpp3Dom strategyNode = mock(Xpp3Dom.class);
        when(configDom.getChild(PluginConfig.STRATEGY_NODE_NAME)).thenReturn(strategyNode);
        when(strategyNode.getAttributeNames()).thenReturn(new String[]{});
        when(strategyNode.getChildren()).thenReturn(new Xpp3Dom[]{});
        final String hint = "git";
        when(strategyNode.getAttribute(PluginConfig.STRATEGY_HINT)).thenReturn(hint);
        final VersionStrategy versionStrategy = mock(VersionStrategy.class);
        when(container.lookup(VersionStrategy.ROLE, hint)).thenReturn(versionStrategy);
        final ComponentConfigurator componentConfigurator = mock(ComponentConfigurator.class);
        when(container.lookup(ComponentConfigurator.ROLE, "basic")).thenReturn(componentConfigurator);

        final PluginConfig config = provider.getForProject(project);
        assertThat(config).isNotNull();
        assertThat(config.shouldGenerateTemporaryFile).isFalse();
        assertThat(config.shouldDeleteTemporaryFile).isTrue();
        assertThat(config.shouldUpdateDependencies).isFalse();
        assertThat(config.versionStrategy).isEqualTo(versionStrategy);

        verify(componentConfigurator).configureComponent(
                eq(versionStrategy),
                any(PlexusConfiguration.class),
                any(ExpressionEvaluator.class),
                eq(null),
                eq(null)
        );
    }
}
