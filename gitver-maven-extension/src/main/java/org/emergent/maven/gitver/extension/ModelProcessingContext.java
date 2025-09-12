package org.emergent.maven.gitver.extension;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.maven.building.Source;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.emergent.maven.gitver.core.ArtifactCoordinates;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.VersionConfig;
import org.emergent.maven.gitver.core.git.GitExec;
import org.emergent.maven.gitver.core.git.GitUtil;
import org.emergent.maven.gitver.core.version.VersionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles calculating version properties from the Git history.
 */
public class ModelProcessingContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(ModelProcessingContext.class);

  private final Set<Path> relatedPoms = new HashSet<>();
  private final AtomicReference<VersionStrategy> strategyRef = new AtomicReference<>();

  public ModelProcessingContext() {
  }

  public Model processModel(Model projectModel, Map<String, ?> options) {
    if (Util.isDisabled()) {
      return projectModel;
    }

    final Source pomSource = (Source)options.get(ModelProcessor.SOURCE);
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
    Path dotmvnDirectory = getDOTMVNDirectory(projectModel.getProjectDirectory().toPath());
    VersionConfig versionConfig = loadConfig(dotmvnDirectory);
    ArtifactCoordinates extensionGAV = Util.extensionArtifact();
    LOGGER.info(
      MessageUtils.buffer()
        .a("--- ")
        .mojo(extensionGAV)
        .a(" ")
        .strong("[core-extension]")
        .a(" ---")
        .build());
    VersionStrategy versionStrategy = GitUtil.getVersionStrategy(projectModel.getProjectDirectory(), versionConfig);
    findRelatedProjects(projectModel);
    printProperties(versionStrategy.toProperties());
    return versionStrategy;
  }

  private void findRelatedProjects(Model projectModel) {
    Path projectPath = projectModel.getProjectDirectory().toPath();
    LOGGER.debug("Finding related projects for {} {}", projectModel.getArtifactId(), projectPath);

    // Add main project
    relatedPoms.add(projectModel.getPomFile().toPath());

    // Find modules
    List<Path> modulePoms = projectModel.getModules().stream()
        .map(module -> projectPath.resolve(module).resolve("pom.xml"))
        .toList();
    LOGGER.debug("Modules found: {}", modulePoms.stream().map(Path::toString)
      .collect(Collectors.joining("\n", "\n", "")));
    relatedPoms.addAll(modulePoms);
  }

  private void processRelatedProjects(Model projectModel, VersionStrategy versionStrategy) {
    String versionString = versionStrategy.toVersionString();
    Map<String, String> strategyProperties = versionStrategy.toProperties();

    Path modelPath = projectModel.getProjectDirectory().toPath();
    File modelPomFile = projectModel.getPomFile();
    Path modelPomPath = modelPomFile.toPath();
    LOGGER.debug("Processing model for {}", modelPomPath);

    if (!relatedPoms.contains(modelPomPath)) return;
    if (projectModel.getProperties().contains("gitver.version")) {
      LOGGER.warn("Skipping repeat of {}", modelPomPath);
      return;
    }

    LOGGER.debug("Project {}:{}, Computed version: {}",
      getGroupId(projectModel),
      projectModel.getArtifactId(),
      MessageUtils.buffer().strong(versionString));

    if (projectModel.getVersion() != null) {
      projectModel.setVersion(versionString);
    }

    Optional.ofNullable(projectModel.getParent()).ifPresent(parent -> {
      Path parentPomPath = modelPath.resolve(parent.getRelativePath()).normalize();
      if (Files.exists(parentPomPath) && relatedPoms.contains(parentPomPath)) {
        LOGGER.info("Setting parent {} version to {}", parent, versionString);
        parent.setVersion(versionString);
      } else {
        LOGGER.debug("Parent {} is not part of this build. Skipping version change for parent.", parent);
      }
    });

    addGitverProperties(projectModel, strategyProperties);
    addVersionerBuildPlugin(projectModel);
  }

  private VersionConfig loadConfig(Path dotmvnDirectory) {
    Properties fileProps = loadExtensionProperties(dotmvnDirectory);
    try (StringWriter writer = new StringWriter()) {
      fileProps.store(writer, null);
      LOGGER.debug(String.format("VersionConfig:%n%s%n", writer));
    } catch (IOException e) {
      throw new GitverException(e.getMessage(), e);
    }

    return VersionConfig.from(fileProps);
  }

  private Properties loadExtensionProperties(Path dotmvnDirectory) {
    Properties props = new Properties();
    Path propertiesPath = dotmvnDirectory.resolve(Util.GITVER_EXTENSION_PROPERTIES);
    if (propertiesPath.toFile().exists()) {
      LOGGER.info("Reading gitver properties from {}", propertiesPath);
      try (Reader reader = Files.newBufferedReader(propertiesPath)) {
        props.load(reader);
      } catch (IOException e) {
        throw new GitverException("Failed to load extensions properties file", e);
      }
    } else {
      LOGGER.debug("Unfound gitver properties at {}", propertiesPath);
    }
    return props;
  }

  private void addGitverProperties(Model projectModel, Map<String, String> properties) {
    projectModel.getProperties().putAll(properties);
  }

  private static void printProperties(Map<String, String> flattened) {
    MessageBuilder builder = MessageUtils.buffer().a("properties:");
    flattened.forEach((k, v) -> builder.newline().format("  %s=%s", k, v));
    LOGGER.info("Adding generated properties to project model: {}", builder);
  }

  private static Path getDOTMVNDirectory(Path currentDir) {
    LOGGER.info("Finding .mvn in {}", currentDir);
    return Util.getDOTMVNDirectory(currentDir);
  }

  private static String getGroupId(Model projectModel) {
    return (projectModel.getGroupId() == null && projectModel.getParent() != null)
      ? projectModel.getParent().getGroupId()
      : projectModel.getGroupId();
  }

  private void addVersionerBuildPlugin(Model projectModel) {
    ArtifactCoordinates extensionGAV = Util.extensionArtifact();
    LOGGER.debug("Adding build plugin version {}", extensionGAV);
    if (projectModel.getBuild() == null) {
      projectModel.setBuild(new Build());
    }
    if (projectModel.getBuild().getPlugins() == null) {
      projectModel.getBuild().setPlugins(new ArrayList<>());
    }
    Plugin plugin = new Plugin();

    plugin.setGroupId(extensionGAV.getGroupId());
    plugin.setArtifactId(extensionGAV.getArtifactId().replace("-extension", "-plugin"));
    plugin.setVersion(extensionGAV.getVersion());
    Plugin existing = projectModel.getBuild().getPluginsAsMap().get(plugin.getKey());
    boolean addExecution = false;

    if (existing != null) {
      plugin = existing;
      LOGGER.info("Found existing plugin configuration for {}", plugin.getKey());
      if (!existing.getVersion().equals(extensionGAV.getVersion())) {
        LOGGER.warn(
          MessageUtils.buffer()
            .mojo(plugin)
            .warning(" version is different than ")
            .mojo(extensionGAV)
            .newline()
            .a("This can introduce unexpected behaviors.")
            .build());
      }
//      Optional<PluginExecution> setGoal =
//        existing.getExecutions().stream().filter(e -> e.getGoals().contains("set")).findFirst();
//      if (setGoal.isPresent()) {
//        LOGGER.info("Using existing plugin execution with id {}", setGoal.get().getId());
//        addExecution = false;
//      }
    } else {
//      addPluginConfiguration(plugin, versionConfig);
      projectModel.getBuild().getPlugins().add(0, plugin);
    }

//    if (addExecution) {
//      LOGGER.debug("Adding build plugin execution for {}", plugin.getKey());
//      PluginExecution execution = new PluginExecution();
//      execution.setId("gitver-set");
//      execution.setGoals(Collections.singletonList("set"));
//      plugin.addExecution(execution);
//    }
//    if (existing == null) projectModel.getBuild().getPlugins().add(0, plugin);
  }

  private void addPluginConfiguration(Plugin plugin, VersionConfig versionConfig) {
    // Version keywords are used by version commit goals.
    String config = String.format(//language=xml
      """
          <configuration>
            <majorKey>%s</majorKey>
            <minorKey>%s</minorKey>
            <patchKey>%s</patchKey>
            <useRegex>%s</useRegex>
          </configuration>
        """,
      versionConfig.getMajorKey(),
      versionConfig.getMinorKey(),
      versionConfig.getPatchKey(),
      versionConfig.isUseRegex());
    try {
      Xpp3Dom configDom = Xpp3DomBuilder.build(new StringReader(config));
      plugin.setConfiguration(configDom);
    } catch (XmlPullParserException | IOException e) {
      throw new GitverException(e.getMessage(), e);
    }
  }
}
