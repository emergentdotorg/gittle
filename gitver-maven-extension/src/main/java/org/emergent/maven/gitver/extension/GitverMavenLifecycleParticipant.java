package org.emergent.maven.gitver.extension;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.Util;
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
  public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
    super.afterProjectsRead(session);
    updateProjects(session);
  }

  private void updateProjects(MavenSession session) {
    session.getAllProjects().forEach(this::updateProject);
  }

  private void updateProject(MavenProject project) {
    if (Util.isDisabled()) {
      LOGGER.debug("{} is disabled", getClass().getSimpleName());
      return;
    }

    Model model = project.getModel();
    Path oldPom = model.getPomFile().toPath();
    Model newModel = readPom(oldPom);
    Optional.ofNullable(newModel.getVersion()).ifPresent(v -> {
      newModel.setVersion(model.getVersion());
    });
    Optional.ofNullable(newModel.getParent()).ifPresent(p -> {
      p.setVersion(model.getParent().getVersion());
    });
//    Properties props = model.getProperties();
//    props.entrySet().stream()
//      .filter(entry -> entry.getKey() instanceof String && entry.getValue() instanceof String)
//      .map(entry -> Maps.immutableEntry(String.valueOf(entry.getKey()), String.valueOf(entry.getValue())))
//      .filter(entry -> entry.getKey() != null && entry.getKey().startsWith("gitver."))
//      .forEach(entry -> newModel.addProperty(entry.getKey(), entry.getValue()));
    Path newPom = oldPom.resolveSibling(Util.GITVER_POM_XML);
    writePomx(newModel, newPom);
    LOGGER.debug("Generated gitver pom at {}", newPom.toAbsolutePath());
    if (newPom.toFile().exists()) {
      LOGGER.debug("Updating project with gitver pom {}", newPom.toAbsolutePath());
      project.setPomFile(newPom.toFile());
    }
  }

  private static Model readPom(Path pomPath) {
    try (InputStream inputStream = Files.newInputStream(pomPath)) {
      MavenXpp3Reader reader = new MavenXpp3Reader();
      return reader.read(inputStream);
    } catch (IOException | XmlPullParserException e) {
      throw new GitverException(e.getMessage(), e);
    }
  }

  private static void writePomx(Model projectModel, Path newPomPath) {
    try (Writer fileWriter = Files.newBufferedWriter(newPomPath, Charset.defaultCharset())) {
      MavenXpp3Writer writer = new MavenXpp3Writer();
      writer.write(fileWriter, projectModel);
    } catch (IOException e) {
      throw new GitverException(e.getMessage(), e);
    }
  }

}
