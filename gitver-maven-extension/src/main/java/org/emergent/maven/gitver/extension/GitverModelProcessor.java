package org.emergent.maven.gitver.extension;

import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;
import static org.emergent.maven.gitver.core.Util.join;
import static org.emergent.maven.gitver.extension.ExtensionUtil.$_REVISION;
import static org.emergent.maven.gitver.extension.ExtensionUtil.REVISION;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.building.Source;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.building.ModelProcessor;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.sisu.Priority;
import org.eclipse.sisu.Typed;
import org.emergent.maven.gitver.core.Coordinates;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.PropCodec;
import org.emergent.maven.gitver.core.version.StrategyFactory;
import org.emergent.maven.gitver.core.version.VersionStrategy;

/**
 * Handles calculating version properties from the Git history.
 */
@Slf4j
@Priority(1)
@Named("core-default")
@Singleton
@Typed(ModelProcessor.class)
public class GitverModelProcessor extends DefaultModelProcessor {

    private final Set<Path> relatedPoms = new HashSet<>();
    private final AtomicReference<VersionStrategy> strategyRef = new AtomicReference<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final boolean addProperties;
    private final boolean addPlugin;
    private final boolean configurePlugin;

    public GitverModelProcessor() {
        addProperties = true;
        addPlugin = false;
        configurePlugin = false;
    }

    @Override
    public Model read(File input, Map<String, ?> options) throws IOException {
        return processModel(super.read(input, options), options);
    }

    @Override
    public Model read(Reader input, Map<String, ?> options) throws IOException {
        return processModel(super.read(input, options), options);
    }

    @Override
    public Model read(InputStream input, Map<String, ?> options) throws IOException {
        return processModel(super.read(input, options), options);
    }

    public Model processModel(Model projectModel, Map<String, ?> options) {
        if (Util.isDisabled()) {
            if (initialized.compareAndSet(false, true)) {
                log.debug("{} is disabled", getClass().getSimpleName());
            }
            return projectModel;
        }

        Source pomSource = (Source) options.get(ModelProcessor.SOURCE);
        Optional<String> pomLoc = Optional.ofNullable(pomSource).map(Source::getLocation);
        pomLoc.ifPresent(loc -> projectModel.setPomFile(new File(loc)));
        if (pomLoc.filter(loc -> loc.endsWith(".xml")).isEmpty()) {
            // Source poms end with .xml but dependency poms end with .pom
            return projectModel;
        }

        // This model processor is invoked for every POM on the classpath, including the plugins.
        // The first execution is with the project's pom though. We use strategyRef to avoid processing other poms.
        VersionStrategy strategy = strategyRef.updateAndGet(curStrat -> {
            if (curStrat != null) {
                return curStrat;
            }
            return getVersionStrategy(projectModel);
        });

        processRelatedProjects(projectModel, strategy);
        return projectModel;
    }

    private VersionStrategy getVersionStrategy(Model projectModel) {
        GitverConfig config = loadConfig(projectModel);
        Coordinates extensionGAV = Util.getExtensionCoordinates();
        log.info(buffer().a("--- ")
                .mojo(extensionGAV)
                .a(" ")
                .strong("[core-extension]")
                .a(" ---")
                .build());
        File basedir = projectModel.getProjectDirectory();
        VersionStrategy versionStrategy = StrategyFactory.getVersionStrategy(basedir, config);
        findRelatedProjects(projectModel);
        return versionStrategy;
    }

    private GitverConfig loadConfig(Model projectModel) {
        Path currentDir = projectModel.getProjectDirectory().toPath().toAbsolutePath();
        Path extConfigFile = Util.getExtensionPropsFile(currentDir);
        Properties fileProps = Util.loadPropsFromFile(extConfigFile);
        log.info("Loaded configuration from file {}:{}", extConfigFile, join(fileProps));
        GitverConfig config = GitverConfig.from(fileProps);
        if (!fileProps.equals(config.toProperties())) {
            log.warn("Round-trip configuration to properties:{}", join(config.toProperties()));
        }
        return config;
    }

    private void findRelatedProjects(Model model) {
        Path basedir = model.getProjectDirectory().toPath();
        log.debug("Finding related projects for {} {}", model.getArtifactId(), basedir);
        // Add main project absolute path
        relatedPoms.add(model.getPomFile().toPath());
        // Find modules
        List<Path> modulePoms = model.getModules().stream()
                .map(module -> basedir.resolve(module).resolve("pom.xml"))
                .toList();
        log.debug(
                "Modules found:{}",
                modulePoms.stream().map(Path::toString).collect(Collectors.joining("\n", "\n", "")));
        relatedPoms.addAll(modulePoms);
    }

    private void processRelatedProjects(Model model, VersionStrategy strategy) {
        String versionString = strategy.toVersionString();

        Path modelPomPath =
                Optional.ofNullable(model.getPomFile()).map(File::toPath).orElse(null);
        if (modelPomPath == null || !relatedPoms.contains(modelPomPath)) {
            return;
        }
        log.debug("Processing model for {}", modelPomPath);

        Coordinates projectGav = Coordinates.builder()
                .setGroupId(getGroupId(model).orElse(""))
                .setArtifactId(model.getArtifactId())
                .setVersion(versionString)
                .build();

        log.debug(
                "Project {}:{}, Computed version: {}",
                projectGav.getGroupId(),
                projectGav.getArtifactId(),
                buffer().strong(projectGav.getVersion()));

        Optional<String> projectVersion = Optional.ofNullable(model.getVersion());
        if (projectVersion.filter($_REVISION::equals).isEmpty()) {
            model.setVersion(versionString);
        }

        Optional<String> parentVersion = Optional.ofNullable(model.getParent()).map(Parent::getVersion);
        Optional.ofNullable(model.getParent())
                .filter(parent -> !$_REVISION.equals(parent.getVersion()))
                .filter(parent -> Objects.nonNull(parent.getRelativePath()))
                .ifPresent(parent -> {
                    Path parentPath = model.getProjectDirectory().toPath().resolve(parent.getRelativePath());
                    if (Files.exists(parentPath) && relatedPoms.contains(parentPath.normalize())) {
                        log.info("Setting parent {} version to {}", parent, versionString);
                        parent.setVersion(versionString);
                    } else {
                        log.debug("Parent {} is not part of this build. Skipping version change for parent.", parent);
                    }
                });

        if (addProperties) {
            Properties newProps = strategy.toProperties();
            log.info(
                    "Adding properties to project {}", buffer().mojo(projectGav).a(join(newProps)));
            model.getProperties().putAll(newProps);
        }
        if ($_REVISION.equals(projectVersion.orElse(parentVersion.orElse(null)))) {
            model.getProperties().put(REVISION, versionString);
        }
        if (addPlugin) {
            addBuildPlugin(model, strategy);
        }
    }

    private void addBuildPlugin(Model projectModel, VersionStrategy strategy) {
        Coordinates coordinates = Util.getPluginCoordinates();
        log.debug("Adding build plugin version {}", coordinates);

        Build build = Optional.ofNullable(projectModel.getBuild()).orElseGet(() -> {
            projectModel.setBuild(new Build());
            return projectModel.getBuild();
        });
        if (Optional.ofNullable(build.getPlugins()).isEmpty()) {
            build.setPlugins(new ArrayList<>());
        }
        PluginManagement pluginMgmt = Optional.ofNullable(build.getPluginManagement())
                .orElseGet(() -> {
                    build.setPluginManagement(new PluginManagement());
                    return build.getPluginManagement();
                });
        if (Optional.ofNullable(pluginMgmt.getPlugins()).isEmpty()) {
            pluginMgmt.setPlugins(new ArrayList<>());
        }

        Plugin plugin = new Plugin();
        plugin.setGroupId(coordinates.getGroupId());
        plugin.setArtifactId(coordinates.getArtifactId());
        plugin.setVersion(coordinates.getVersion());
        String key = plugin.getKey();

        Plugin normPlugin = build.getPluginsAsMap().get(key);
        Plugin mgmtPlugin = pluginMgmt.getPluginsAsMap().get(key);
        Optional<Plugin> found =
                Stream.of(normPlugin, mgmtPlugin).filter(Objects::nonNull).findFirst();

        found.ifPresent(existing -> log.warn(buffer().mojo(existing)
                .warning(" version is different than ")
                .mojo(coordinates)
                .newline()
                .a("This can introduce unexpected behaviors.")
                .build()));

        if (found.isEmpty()) {
            if (configurePlugin) {
                addPluginConfiguration(plugin, strategy.getConfig());
            }
            pluginMgmt.getPlugins().add(0, plugin);
        }
    }

    private static void addPluginConfiguration(Plugin plugin, GitverConfig config) {
        String serialized = PropCodec.getInstance().toStringStringMap(config.toProperties()).entrySet().stream()
          .map(GitverModelProcessor::element).collect(Collectors.joining());
        if (!serialized.isEmpty()) {
            try {
                plugin.setConfiguration(Xpp3DomBuilder.build(new StringReader(String.format(
                        """
                      <configuration>
                        %s
                      </configuration>
                    """,
                        serialized))));
            } catch (XmlPullParserException | IOException e) {
                throw new GitverException(e.getMessage(), e);
            }
        }
    }

    private static String element(Entry<String, String> e) {
        return "<" + e.getKey() + ">" + e.getValue() + "<" + e.getKey() + ">";
    }

    private static Optional<String> getGroupId(Model projectModel) {
        Optional<String> groupId = Optional.ofNullable(projectModel.getGroupId());
        if (groupId.isEmpty()) {
            groupId = Optional.ofNullable(projectModel.getParent()).map(Parent::getGroupId);
        }
        return groupId;
    }
}
