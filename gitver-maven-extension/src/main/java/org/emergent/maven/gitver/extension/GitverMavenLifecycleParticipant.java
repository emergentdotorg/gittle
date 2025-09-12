package org.emergent.maven.gitver.extension;

import com.google.common.collect.Maps;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles creating the updated pom file, and assigning it to the project model.
 */
@Named("gitver-lifecycle-participant")
@Singleton
public class GitverMavenLifecycleParticipant extends AbstractMavenLifecycleParticipant {

  private static final Logger LOGGER = LoggerFactory.getLogger(GitverMavenLifecycleParticipant.class);

  @Override
  public void afterSessionStart(MavenSession session) throws MavenExecutionException {
    super.afterSessionStart(session);
  }

  @Override
  public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
    super.afterProjectsRead(session);
    updateProjects(session);
  }

  @Override
  public void afterSessionEnd(MavenSession session) throws MavenExecutionException {
    super.afterSessionEnd(session);
  }

  private void updateProjects(MavenSession session) {
    session.getAllProjects().forEach(this::updateProject);
  }

  private void updateProject(MavenProject project) {
    if (Util.isDisabled()) {
      return;
    }

    Model model = project.getModel();
    Path oldPom = model.getPomFile().toPath();
    Model newModel = Util.readPom(oldPom);
    Optional.ofNullable(newModel.getVersion()).ifPresent(v -> {
      newModel.setVersion(model.getVersion());
    });
    Optional.ofNullable(newModel.getParent()).ifPresent(p -> {
      p.setVersion(model.getParent().getVersion());
    });
    Properties props = model.getProperties();
    props.entrySet().stream()
      .filter(entry -> entry.getKey() instanceof String && entry.getValue() instanceof String)
      .map(entry -> Maps.immutableEntry(String.valueOf(entry.getKey()), String.valueOf(entry.getValue())))
      .filter(entry -> entry.getKey() != null && entry.getKey().startsWith("gitver."))
      .forEach(entry -> newModel.addProperty(entry.getKey(), entry.getValue()));
    Path newPom = oldPom.resolveSibling(Util.GITVER_POM_XML);
    Util.writePomx(newModel, newPom);
    LOGGER.debug("Generated gitver pom at {}", newPom.toAbsolutePath());
    if (newPom.toFile().exists()) {
      LOGGER.debug("Updating project with gitver pom {}", newPom.toAbsolutePath());
      project.setPomFile(newPom.toFile());
    }
  }
}
