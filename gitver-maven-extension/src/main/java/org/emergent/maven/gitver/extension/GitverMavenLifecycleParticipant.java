package org.emergent.maven.gitver.extension;

import static org.emergent.maven.gitver.core.Util.GITVER_POM_XML;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
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
    private final AtomicBoolean initialized = new AtomicBoolean(false);

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
                LOGGER.debug("{} is disabled", getClass().getSimpleName());
            }
            return;
        }
        Model originalModel = project.getModel();
        Path originalPomFile = originalModel.getPomFile().toPath().toAbsolutePath();
        Path gitverPomFile = originalPomFile.resolveSibling(GITVER_POM_XML);
        try {
            Model gitverModel = readModelFromPom(originalPomFile);
            // Update the new model with versions for artifact and parent if needed.
            String newVersion = originalModel.getVersion();
            if (Objects.nonNull(gitverModel.getVersion())) gitverModel.setVersion(newVersion);
            Optional.ofNullable(gitverModel.getParent()).ifPresent(parent -> parent.setVersion(newVersion));
            // Now write the updated model out to a file so we can point the project to it.
            writeModelToPom(gitverModel, gitverPomFile);
            project.setPomFile(gitverPomFile.toFile());
            LOGGER.debug("Updated project with newly generated gitver pom {}", gitverPomFile);
        } catch (Exception e) {
            LOGGER.error("Failed creating new gitver pom at {}", gitverPomFile, e);
        }
    }

    private static Model readModelFromPom(Path pomPath) {
        try (InputStream inputStream = Files.newInputStream(pomPath)) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            return reader.read(inputStream);
        } catch (IOException | XmlPullParserException e) {
            throw new GitverException(e.getMessage(), e);
        }
    }

    private static void writeModelToPom(Model projectModel, Path newPomPath) {
        try (Writer fileWriter = Files.newBufferedWriter(newPomPath, Charset.defaultCharset())) {
            MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(fileWriter, projectModel);
        } catch (IOException e) {
            throw new GitverException(e.getMessage(), e);
        }
    }
}
