package org.emergent.maven.gitver.extension;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.building.Source;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.building.ModelProcessor;
import org.apache.maven.shared.utils.logging.MessageBuilder;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.sisu.Priority;
import org.eclipse.sisu.Typed;
import org.emergent.maven.gitver.core.ArtifactCoordinates;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.VersionConfig;
import org.emergent.maven.gitver.core.git.GitUtil;
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
      LOGGER.debug("{} is disabled", getClass().getSimpleName());
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
    VersionConfig versionConfig = loadConfig(projectModel);
    ArtifactCoordinates extensionGAV = Util.getExtensionCoordinates();
    LOGGER.info(
      MessageUtils.buffer()
        .a("--- ")
        .mojo(extensionGAV)
        .a(" ")
        .strong("[core-extension]")
        .a(" ---")
        .build());
    GitUtil gitUtil = GitUtil.getInstance(projectModel.getProjectDirectory());
    projectModel.getProjectDirectory();
    VersionStrategy versionStrategy = gitUtil.getVersionStrategy(versionConfig);
    findRelatedProjects(projectModel);
    printProperties(versionStrategy.toProperties());
    return versionStrategy;
  }

  private VersionConfig loadConfig(Model projectModel) {
    Path dotmvnDirectory = getDOTMVNDirectory(projectModel.getProjectDirectory().toPath());
    Properties fileProps = loadExtensionProperties(dotmvnDirectory);
//    VersionConfig versionConfig = loadConfig(dotmvnDirectory);
//    Properties fallback = new Properties();
//    versionConfig.toProperties().forEach(fallback::setProperty);
    Properties props = new Properties(fileProps);
    props.putAll(Util.flatten(projectModel.getProperties()));
    VersionConfig vc = VersionConfig.from(props);
    return vc;
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
//    addBuildPlugin(projectModel);
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
    return Util.loadExtensionProperties(dotmvnDirectory);
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

  private void addBuildPlugin(Model projectModel) {
    ArtifactCoordinates coordinates = Util.getPluginCoordinates();
    LOGGER.debug("Adding build plugin version {}", coordinates);
    if (projectModel.getBuild() == null) {
      projectModel.setBuild(new Build());
    }
    if (projectModel.getBuild().getPlugins() == null) {
      projectModel.getBuild().setPlugins(new ArrayList<>());
    }
    Plugin plugin = new Plugin();

    plugin.setGroupId(coordinates.getGroupId());
    plugin.setArtifactId(coordinates.getArtifactId());
    plugin.setVersion(coordinates.getVersion());
    Plugin existing = projectModel.getBuild().getPluginsAsMap().get(plugin.getKey());
    boolean addExecution = false;

    if (existing != null) {
      plugin = existing;
      LOGGER.info("Found existing plugin configuration for {}", plugin.getKey());
      if (!existing.getVersion().equals(coordinates.getVersion())) {
        LOGGER.warn(
          MessageUtils.buffer()
            .mojo(plugin)
            .warning(" version is different than ")
            .mojo(coordinates)
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
    String configXml = String.format(//language=xml
      """
          <configuration>
            <majorKey>%s</majorKey>
            <minorKey>%s</minorKey>
            <patchKey>%s</patchKey>
            <useRegex>%s</useRegex>
          </configuration>
        """,
      versionConfig.getMajorKeywords(),
      versionConfig.getMinorKeywords(),
      versionConfig.getPatchKeywords(),
      versionConfig.isRegexKeywords());
    try {
      Xpp3Dom configDom = Xpp3DomBuilder.build(new StringReader(configXml));
      plugin.setConfiguration(configDom);
    } catch (XmlPullParserException | IOException e) {
      throw new GitverException(e.getMessage(), e);
    }
  }

}
