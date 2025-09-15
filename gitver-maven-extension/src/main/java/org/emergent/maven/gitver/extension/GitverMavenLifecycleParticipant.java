package org.emergent.maven.gitver.extension;

import static org.emergent.maven.gitver.core.Util.GITVER_POM_XML;
import static org.emergent.maven.gitver.extension.ExtensionUtil.$_REVISION;
import static org.emergent.maven.gitver.extension.ExtensionUtil.REVISION;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;
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
            Model gitverModel = ExtensionUtil.readModelFromPom(originalPomFile);
            copyVersions(originalModel, gitverModel);
            // Now write the updated model out to a file so we can point the project to it.
            ExtensionUtil.writeModelToPom(gitverModel, gitverPomFile);
            project.setPomFile(gitverPomFile.toFile());
            LOGGER.debug("Updated project with newly generated gitver pom {}", gitverPomFile);
        } catch (Exception e) {
            LOGGER.error("Failed creating new gitver pom at {}", gitverPomFile, e);
        }
    }


    @SuppressWarnings("UnusedReturnValue")
    public static boolean copyVersions(Model sourceModel, Model targetModel) {
        Optional<Model> source = Optional.ofNullable(sourceModel);

        Optional<String> sourceVersion = source.map(Model::getVersion);
        boolean versionUpdated = sourceVersion.map(newValue -> {
            String original = targetModel.getVersion();
            targetModel.setVersion(newValue);
            return newValue.equals(original);
        }).orElse(false);

        Optional<String> sourceParentVersion = source.map(Model::getParent).map(Parent::getVersion);
        boolean parentUpdated = sourceParentVersion.map(newValue -> {
            Parent parent = Optional.ofNullable(targetModel.getParent()).orElseGet(() -> {
                targetModel.setParent(new Parent());
                return targetModel.getParent();
            });
            String original = parent.getVersion();
            parent.setVersion(newValue);
            return newValue.equals(original);
        }).orElse(false);

        Optional<Object> sourceRevisionProperty = source.map(Model::getProperties).map(p -> p.get(REVISION));
        boolean revisionUpdated = sourceRevisionProperty.map(newValue -> {
            Properties properties = Optional.ofNullable(targetModel.getProperties()).orElseGet(() -> {
                targetModel.setProperties(new Properties());
                return targetModel.getProperties();
            });
            Object original = properties.put($_REVISION, newValue);
            return !newValue.equals(original);
        }).orElse(false);

        return versionUpdated | parentUpdated | revisionUpdated;
    }
}
