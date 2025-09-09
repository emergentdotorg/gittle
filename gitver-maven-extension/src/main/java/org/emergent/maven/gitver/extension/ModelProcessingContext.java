package org.emergent.maven.gitver.extension;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.emergent.maven.gitver.core.ArtifactCoordinates;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.GitVerConfig;
import org.emergent.maven.gitver.core.git.GitUtil;
import org.emergent.maven.gitver.core.version.VersionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.emergent.maven.gitver.extension.Util.DOT_MVN;

/**
 * Handles calculating version properties from the Git history.
 */
public class ModelProcessingContext {

  private static final ModelProcessingContext INSTANCE = new ModelProcessingContext();

  private static final Logger LOGGER = LoggerFactory.getLogger(ModelProcessingContext.class);

  private final Set<Path> relatedPoms = new HashSet<>();
  private final AtomicReference<VersionStrategy> strategyRef = new AtomicReference<>();
  private final Map<Path, Model> pomPathToModelCache = new HashMap<>();

  private ModelProcessingContext() {}

  public static ModelProcessingContext getInstance() {
    return INSTANCE;
  }

  public Model processModel(Model projectModel, Map<String, ?> options) {
     if (Util.isDisabled()) {
       return projectModel;
     }

    final Source pomSource = (Source)options.get(ModelProcessor.SOURCE);

    if (pomSource != null) {
      projectModel.setPomFile(new File(pomSource.getLocation()));
    }

    // Only source poms are with .xml dependency poms are with .pom
    if (pomSource == null || !pomSource.getLocation().endsWith(".xml")) {
      return projectModel;
    }

    cacheOriginalModel(projectModel);

    // This model processor is invoked for every POM on the classpath, including the plugins.
    // The first execution is with the project's pom though. Use first initialized flag to avoid processing other poms.

    VersionStrategy strategy = strategyRef.updateAndGet(curStrat -> {
      if (curStrat != null) return curStrat;
      Path dotmvnDirectory = ModelProcessingContext.getDOTMVNDirectory(projectModel.getProjectDirectory().toPath());
      GitVerConfig config = ModelProcessingContext.loadConfig(dotmvnDirectory);
      return getVersionStrategy(projectModel, config);
    });

    processRelatedProjects(projectModel, strategy);
    return projectModel;
  }

  public Model lookupOriginalModel(Model model) {
    return pomPathToModelCache.get(model.getPomFile().toPath().toAbsolutePath());
  }

  private void cacheOriginalModel(Model model) {
    pomPathToModelCache.put(model.getPomFile().toPath().toAbsolutePath(), model);
  }

  private VersionStrategy getVersionStrategy(Model projectModel, GitVerConfig versionConfig) {
    ArtifactCoordinates extensionGAV = Util.extensionArtifact();
    LOGGER.info(
      MessageUtils.buffer()
        .a("--- ")
        .mojo(extensionGAV)
        .a(" ")
        .strong("[core-extension]")
        .a(" ---")
        .build());
    VersionStrategy versionStrategy = GitUtil.getVersionStrategy(projectModel.getPomFile(), versionConfig);
    findRelatedProjects(projectModel);
    return versionStrategy;
  }

  private void findRelatedProjects(Model projectModel) {
    LOGGER.debug("Finding related projects for {}", projectModel.getArtifactId());

    // Add main project
    relatedPoms.add(projectModel.getPomFile().toPath());

    // Find modules
    List<Path> modulePoms =
      projectModel.getModules().stream()
        .map(
          module ->
            projectModel
              .getProjectDirectory()
              .toPath()
              .resolve(module)
              .resolve("pom.xml")
              .toAbsolutePath())
        .collect(Collectors.toList());
    LOGGER.debug("Modules found: {}", modulePoms);
    relatedPoms.addAll(modulePoms);
  }

  private void processRelatedProjects(Model projectModel, VersionStrategy versionStrategy) {
    File modelPomFile = projectModel.getPomFile();
    LOGGER.info("Processing model for {}", modelPomFile.getAbsolutePath());

    if (!relatedPoms.contains(modelPomFile.toPath())) return;
    LOGGER.info(
      "Project {}:{}, Computed version: {}",
      getGroupId(projectModel),
      projectModel.getArtifactId(),
      MessageUtils.buffer().strong(versionStrategy.toVersionString()));
    projectModel.setVersion(versionStrategy.toVersionString());

    Parent parent = projectModel.getParent();
    if (parent != null) {
      Path path = Paths.get(parent.getRelativePath());
      // Parent is part of this build
      try {
        Path parentPomPath =
          Paths.get(
            modelPomFile
              .getParentFile()
              .toPath()
              .resolve(path)
              .toFile()
              .getCanonicalPath());
        LOGGER.debug("Looking for parent pom {}", parentPomPath);
        if (Files.exists(parentPomPath) && relatedPoms.contains(parentPomPath)) {
          LOGGER.info("Setting parent {} version to {}", parent,
            versionStrategy.toVersionString());
          parent.setVersion(versionStrategy.toVersionString());
        } else {
          LOGGER.debug(
            "Parent {} is not part of this build. Skipping version change for parent.", parent);
        }
      } catch (IOException e) {
        throw new GitverException(e.getMessage(), e);
      }
    }
    addGitverProperties(projectModel, versionStrategy);
  }

  public static GitVerConfig loadConfig(Path dotmvnDirectory) {
    Properties fileProps = loadExtensionProperties(dotmvnDirectory);
    try (StringWriter writer = new StringWriter()) {
      fileProps.store(writer, null);
      LOGGER.warn(String.format("VersionConfig:%n%s%n", writer));
    } catch (IOException e) {
      throw new GitverException(e.getMessage(), e);
    }

    return GitVerConfig.from(fileProps);
  }

  private static Properties loadExtensionProperties(Path dotmvnDirectory) {
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

  private void addGitverProperties(Model projectModel, VersionStrategy versionStrategy) {
    Map<String, String> flattened = Util.toProperties(versionStrategy);
    MessageBuilder builder = MessageUtils.buffer().a("properties:");
    flattened.forEach((k,v) -> builder.newline().format("	%s=%s", k, v));
    LOGGER.info("Adding generated properties to project model: {}", builder);
    projectModel.getProperties().putAll(flattened);
  }

  public static Path getDOTMVNDirectory(Path currentDir) {
    LOGGER.info("Finding .mvn in {}", currentDir);
    Path refDir = currentDir;
    while (refDir != null && !Files.exists(refDir.resolve(DOT_MVN))) {
      refDir = refDir.getParent();
    }
    return Optional.ofNullable(refDir).map(r -> r.resolve(DOT_MVN)).orElse(currentDir);
  }

  private static String getGroupId(Model projectModel) {
    return (projectModel.getGroupId() == null && projectModel.getParent() != null)
      ? projectModel.getParent().getGroupId()
      : projectModel.getGroupId();
  }

  private void addVersionerBuildPlugin(Model projectModel, GitVerConfig versionConfigz) {
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
    boolean addExecution = true;
    if (existing != null) {
      plugin = existing;
      LOGGER.warn("Found existing plugin configuration for {}", plugin.getKey());
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
      Optional<PluginExecution> setGoal =
        existing.getExecutions().stream().filter(e -> e.getGoals().contains("set")).findFirst();
      if (setGoal.isPresent()) {
        LOGGER.info("Using existing plugin execution with id {}", setGoal.get().getId());
        addExecution = false;
      }
    }
    addPluginConfiguration(plugin, versionConfigz);
    if (addExecution) {
      LOGGER.debug("Adding build plugin execution for {}", plugin.getKey());
      PluginExecution execution = new PluginExecution();
      execution.setId("gitver-set");
      execution.setGoals(Collections.singletonList("set"));
      plugin.addExecution(execution);
    }
    if (existing == null) projectModel.getBuild().getPlugins().add(0, plugin);
  }

  private void addPluginConfiguration(Plugin plugin, GitVerConfig versionConfig) {
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
