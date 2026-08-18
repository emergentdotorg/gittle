package org.emergent.gittle.maven.extension;

import static org.emergent.gittle.core.Util.GITTLE_POM_XML;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.emergent.gittle.core.Util;
import org.emergent.gittle.core.util.GroupArtifactVersion;
import org.emergent.gittle.core.util.ModelProvider;
import org.emergent.gittle.core.util.PluginConfigProvider;

/**
 * Handles creating the updated pom file, and assigning it to the project model.
 */
@Named("gittle-lifecycle-participant")
@Singleton
@Component(role = AbstractMavenLifecycleParticipant.class, hint = "version-inf")
public class GittleMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

  // private static final Logger LOGGER = LoggerFactory.getLogger(GittleMavenLifecycleParticipant.class);
  private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Requirement
    private org.codehaus.plexus.logging.Logger logger;

    @Requirement
    private PlexusContainer container;

    private Map<GroupArtifactVersion, String> projectGavs = new HashMap<>();

    private PluginConfigProvider pluginConfigProvider;

    private ModelProvider modelProvider;


    @Override
  public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
    super.afterProjectsRead(session);
    updateProjects(session);
  }

  private void updateProjects(MavenSession session) {
    session.getAllProjects().forEach(this::updateProject);
  }

  private void updateProject(MavenProject project) {
    if (Util.isDisabled()) {
      if (initialized.compareAndSet(false, true)) {
        logger.debug(String.format("%s is disabled", getClass().getSimpleName()));
      }
      return;
    }
    Model originalModel = project.getModel();
    Path originalPomFile = originalModel.getPomFile().toPath().toAbsolutePath();
    Path gittlePomFile = originalPomFile.resolveSibling(GITTLE_POM_XML);
    try {
      Model gittleModel = ExtensionUtil.readModelFromPom(originalPomFile);
      ExtensionUtil.copyVersions(originalModel, gittleModel);
      // Now write the updated model out to a file so we can point the project to it.
      ExtensionUtil.writeModelToPom(gittleModel, gittlePomFile);
      project.setPomFile(gittlePomFile.toFile());
      logger.debug("Updated project with newly generated gittle pom " + gittlePomFile);
    } catch (Exception e) {
      logger.error("Failed creating new gittle pom at " + gittlePomFile, e);
    }
  }


}
