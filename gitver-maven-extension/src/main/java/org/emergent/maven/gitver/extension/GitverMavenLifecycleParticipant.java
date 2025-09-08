package org.emergent.maven.gitver.extension;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Handles creating the updated pom file and assigning it to the project model.
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
    String version = model.getVersion();

    Properties properties = model.getProperties();

    Map<String, String> newprops = new TreeMap<>();

    properties.entrySet().stream().filter(e -> String.valueOf(e.getKey()).startsWith("gitver."))
      .forEach(e -> { newprops.put(String.valueOf(e.getKey()), String.valueOf(e.getValue())); });

    Path oldPom = model.getPomFile().toPath();
    Path newPom = oldPom.resolveSibling(Util.GITVER_POM_XML);

    Model updated = Util.readPom(oldPom);
    updated.setVersion(version);
    updated.getProperties().putAll(newprops);

    Util.writePomx(updated, newPom);
    LOGGER.info("Generated gitver pom at {}", newPom.toAbsolutePath());
    if (newPom.toFile().exists()) {
      LOGGER.info("Updating project with gitver pom {}", newPom.toAbsolutePath());
      project.setPomFile(newPom.toFile());
    }
  }
}
