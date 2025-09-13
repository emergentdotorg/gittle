package org.emergent.maven.gitver.extension;

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
import org.apache.maven.building.Source;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.sisu.Priority;
import org.eclipse.sisu.Typed;
import org.emergent.maven.gitver.core.Coordinates;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.version.KeywordsConfig;
import org.emergent.maven.gitver.core.version.StrategyFactory;
import org.emergent.maven.gitver.core.version.VersionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles calculating version properties from the Git history.
 */
@Priority(1)
@Named("core-default")
@Singleton
@Typed(ModelProcessor.class)
public class GitverModelProcessor extends DefaultModelProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(GitverModelProcessor.class);

  private final Set<Path> relatedPoms = new HashSet<>();
  private final AtomicReference<VersionStrategy> strategyRef = new AtomicReference<>();
  private final AtomicBoolean initialized = new AtomicBoolean(false);

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
        LOGGER.debug("{} is disabled", getClass().getSimpleName());
      }
      return projectModel;
    }

    Source pomSource = (Source)options.get(ModelProcessor.SOURCE);
    Optional<String> pomLoc = Optional.ofNullable(pomSource).map(Source::getLocation);
    pomLoc.ifPresent(loc -> projectModel.setPomFile(new File(loc)));
    if (pomLoc.filter(loc -> loc.endsWith(".xml")).isEmpty()) {
      // Source poms end with .xml but dependency poms end with .pom
      return projectModel;
    }

    // This model processor is invoked for every POM on the classpath, including the plugins.
    // The first execution is with the project's pom though. Use first initialized flag to avoid processing other poms.

    VersionStrategy strategy = strategyRef.updateAndGet(curStrat -> {
      if (curStrat != null) return curStrat;
      return getVersionStrategy(projectModel);
    });

    processRelatedProjects(projectModel, strategy);
    return projectModel;
  }

  private VersionStrategy getVersionStrategy(Model projectModel) {
    GitverConfig config = loadConfig(projectModel);
    Coordinates extensionGAV = Util.getExtensionCoordinates();
    LOGGER.info(
      MessageUtils.buffer()
        .a("--- ")
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
    LOGGER.warn("Loading configuration properties from {}", extConfigFile);
    Properties extensionProps = Util.loadPropsFromFile(extConfigFile);
    LOGGER.info("Loaded configuration from file {}", formatProperties(Util.flatten(extensionProps)));
    GitverConfig config = GitverConfig.from(extensionProps);
    LOGGER.info("Round-trip configuration to properties {}", formatProperties(config.toProperties()));
    return config;
  }

  private void findRelatedProjects(Model model) {
    Path basedir = model.getProjectDirectory().toPath();
    LOGGER.debug("Finding related projects for {} {}", model.getArtifactId(), basedir);
    // Add main project
    relatedPoms.add(model.getPomFile().toPath());
    // Find modules
    List<Path> modulePoms = model.getModules().stream()
      .map(module -> basedir.resolve(module).resolve("pom.xml"))
      .toList();
    LOGGER.debug("Modules found: {}", modulePoms.stream().map(Path::toString)
      .collect(Collectors.joining("\n", "\n", "")));
    relatedPoms.addAll(modulePoms);
  }

  private static String formatProperties(Map<String, String> flattened) {
    MessageBuilder builder = MessageUtils.buffer().a("properties:");
    flattened.forEach((k, v) -> builder.newline().format("  %s=%s", k, v));
    return builder.build();
  }

  private void processRelatedProjects(Model projectModel, VersionStrategy versionStrategy) {
    String versionString = versionStrategy.toVersionString();

    Path modelPath = projectModel.getProjectDirectory().toPath();
    File modelPomFile = projectModel.getPomFile();
    Path modelPomPath = modelPomFile.toPath();
    LOGGER.debug("Processing model for {}", modelPomPath);

    if (!relatedPoms.contains(modelPomPath)) return;
    if (projectModel.getProperties().contains("gitver.version")) {
      LOGGER.warn("Skipping repeat of {}", modelPomPath);
      return;
    }

    Optional<Parent> parent = Optional.ofNullable(projectModel.getParent());
    String groupId = projectModel.getGroupId();
    if (groupId == null) {
      groupId = parent.map(Parent::getGroupId).orElse(null);
    }

    LOGGER.debug("Project {}:{}, Computed version: {}",
      groupId, projectModel.getArtifactId(), MessageUtils.buffer().strong(versionString));

    if (projectModel.getVersion() != null) {
      projectModel.setVersion(versionString);
    }

    parent.ifPresent(p -> {
      Path parentPomPath = modelPath.resolve(p.getRelativePath()).normalize();
      if (Files.exists(parentPomPath) && relatedPoms.contains(parentPomPath)) {
        LOGGER.info("Setting parent {} version to {}", p, versionString);
        p.setVersion(versionString);
      } else {
        LOGGER.debug("Parent {} is not part of this build. Skipping version change for parent.", p);
      }
    });

    Map<String, String> strategyProperties = versionStrategy.toProperties();
    LOGGER.info("Adding generated properties to project model: {}", formatProperties(strategyProperties));
    projectModel.getProperties().putAll(strategyProperties);
    addBuildPlugin(projectModel);
  }

  private void addBuildPlugin(Model projectModel) {
    Coordinates coordinates = Util.getPluginCoordinates();
    LOGGER.debug("Adding build plugin version {}", coordinates);

    Build build = Optional.ofNullable(projectModel.getBuild()).orElseGet(() -> {
      projectModel.setBuild(new Build());
      return projectModel.getBuild();
    });
    if (Optional.ofNullable(build.getPlugins()).isEmpty()) {
      build.setPlugins(new ArrayList<>());
    }
    PluginManagement pluginMgmt = Optional.ofNullable(build.getPluginManagement()).orElseGet(() -> {
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
    Optional<Plugin> found = Stream.of(normPlugin, mgmtPlugin).filter(Objects::nonNull).findFirst();

    found.ifPresent(existing -> LOGGER.warn(MessageUtils.buffer()
        .mojo(existing).warning(" version is different than ").mojo(coordinates)
        .newline().a("This can introduce unexpected behaviors.").build()));

    if (found.isEmpty()) {
      pluginMgmt.getPlugins().add(0, plugin);
    }
  }

  private void addPluginConfiguration(Plugin plugin, GitverConfig config) {
    // Version keywords are used by version commit goals.
    KeywordsConfig keywords = config.getKeywords();
    String configXml = String.format(//language=xml
      """
          <configuration>
            <majorKey>%s</majorKey>
            <minorKey>%s</minorKey>
            <patchKey>%s</patchKey>
            <useRegex>%s</useRegex>
          </configuration>
        """,
      keywords.getMajor(),
      keywords.getMinor(),
      keywords.getPatch(),
      keywords.isRegex());
    try {
      Xpp3Dom configDom = Xpp3DomBuilder.build(new StringReader(configXml));
      plugin.setConfiguration(configDom);
    } catch (XmlPullParserException | IOException e) {
      throw new GitverException(e.getMessage(), e);
    }
  }

}
