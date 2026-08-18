/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.emergent.gittle.maven.plugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Named;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.project.MavenProject;
import org.emergent.gittle.maven.plugin.strategy.VersionStrategy;
import org.emergent.gittle.maven.plugin.util.GroupArtifactVersion;
import org.emergent.gittle.maven.plugin.util.ModelProvider;
import org.emergent.gittle.maven.plugin.util.PluginConfig;
import org.emergent.gittle.maven.plugin.util.PluginConfigProvider;
import org.jspecify.annotations.NullMarked;

/**
 * Maven Extension that will update all the projects in the reactor with an externally managed version.
 * <p>
 * This extension MUST be configured as a plugin in order to be configured.
 * <p>
 * 'strategy' - The configuration for an ExternalVersionStrategy.
 * 'hint' -  A component hint to load the ExternalVersionStrategy.
 */
@Slf4j
@Setter
@Named("gittle-verinf")
public class VersionInferenceExtension extends AbstractMavenLifecycleParticipant {

    @Inject
    private ModelProvider modelProvider;

    @Inject
    private PluginConfigProvider pluginConfigProvider;

    private Map<GroupArtifactVersion, String> projectGavs = new HashMap<>();

    @Override
    public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
        updateProjects(session);
    }

    private void updateProjects(MavenSession session) throws MavenExecutionException {
        log.warn("Updating project versions");

        // session.getAllProjects().forEach(this::updateProject);
        for (MavenProject mavenProject : session.getAllProjects()) {
            setProjectVersion(mavenProject);
        }

        // Need to do a second pass here since our projectGavs map is populated now
        for (MavenProject mavenProject : session.getAllProjects()) {
            updateProject(mavenProject);
        }
    }

    private void setProjectVersion(MavenProject project) throws MavenExecutionException {
        // Get the plugin config
        PluginConfig pluginConfig = pluginConfigProvider.getForProject(project);
        if (pluginConfig == null) {
            return;
        }

        // Store the old version before changing it
        String oldVersion = project.getVersion();
        GroupArtifactVersion gav = GroupArtifactVersion.of(project.getGroupId(), project.getArtifactId(), oldVersion);
        // Now use the strategy to figure out the new version
        String newVersion = getNewVersion(pluginConfig.versionStrategy, project);
        log.info("Inferred project version: " + newVersion);
        projectGavs.put(gav, newVersion);

        String oldFinalName = project.getBuild().getFinalName();
        String newFinalName = oldFinalName.replaceFirst(Pattern.quote(oldVersion), newVersion);
        log.info("Inferred project.build.finalName: " + newFinalName);

        VersionRange versionRange = VersionRange.createFromVersion(newVersion);

        // Now that we have the new version, we update the project versions.
        project.setVersion(newVersion);
        project.getArtifact().setVersion(newVersion);
        project.getArtifact().setVersionRange(versionRange);
        project.getBuild().setFinalName(newFinalName);
    }

    private void updateProject(MavenProject project) throws MavenExecutionException {
        PluginConfig pluginConfig = pluginConfigProvider.getForProject(project);
        if (pluginConfig != null) {
            if (pluginConfig.shouldUpdateDependencies) {
                setDependencyVersions(project);
            }
            setParentVersion(project);
            createNewVersionPom(project);
        }
    }

    /**
     * In the case where the plugin is configured to update project dependency
     * versions, we loop through the dependencies of the project and check if
     * that each dependency is actually one of the projects contained in this
     * POM (with the exception of the project itself, since it can never depend
     * on the latest version of itself). If a dependency is a project of this
     * POM, we update its version to the latest inferred version.
     */
    private void setDependencyVersions(MavenProject mavenProject) {
        GroupArtifactVersion projectGav = GroupArtifactVersion.fromMavenProject(mavenProject);
        mavenProject.getDependencies().forEach(dependency -> {
            GroupArtifactVersion dGav = GroupArtifactVersion.from(dependency);
            Optional.of(dGav)
                    .filter(g -> !g.equals(projectGav))
                    .map(projectGavs::get)
                    .ifPresent(newVersion -> {
                        dependency.setVersion(newVersion);
                        log.info(
                                "Setting project " + projectGav + " dependency " + dGav + " to version " + newVersion);
                    });
        });
    }

    private void setParentVersion(MavenProject mavenProject) throws MavenExecutionException {
        // Update model version. The project's version has been updated so we can just use it here.
        Model model = modelProvider.getModel(mavenProject);
        model.setVersion(mavenProject.getVersion());

        // Update model parent version
        Optional.ofNullable(model.getParent()).ifPresent(parent -> {
            GroupArtifactVersion parentGav = GroupArtifactVersion.from(parent);
            Optional.ofNullable(projectGavs.get(parentGav)).ifPresent(parent::setVersion);
        });

        /*
         * At this point, we've only updated the versions of the individual projects.
         * Now we need to update the references between the updated projects.
         */
        Optional.ofNullable(mavenProject.getParent()).ifPresent(parent -> {
            if (projectGavs.containsKey(GroupArtifactVersion.from(parent))) {
                // We need to update the parent
                //TODO: implement
                log.warn("Need to update parent (not implemented)");
            }
        });
    }

    @NullMarked
    private String getNewVersion(VersionStrategy strategy, MavenProject project) throws MavenExecutionException {
        Optional<String> newVersion;
        try {
            newVersion = Optional.ofNullable(strategy.getVersion(project));
        } catch (VersionException e) {
            throw new MavenExecutionException(e.getMessage(), e);
        }

        return newVersion.orElseThrow(() -> {
            String msg = "Unable to infer new version; strategy returned null.";
            return new MavenExecutionException(msg, project.getFile());
        }).trim();
    }

    private void createNewVersionPom(MavenProject mavenProject) throws MavenExecutionException {
        PluginConfig pluginConfig = pluginConfigProvider.getForProject(mavenProject);

        File newPom = getNewPomFile(mavenProject, pluginConfig);

        if (pluginConfig.shouldDeleteTemporaryFile) {
            newPom.deleteOnExit();
        }

        if (log.isDebugEnabled()) {
            log.debug(VersionInferenceExtension.class.getSimpleName() + ": using new pom file => " + newPom);
        }

        Model model = modelProvider.getModel(mavenProject);

        // Write the new pom to disk
        try (Writer fileWriter = new FileWriter(newPom)) {
            new MavenXpp3Writer().write(fileWriter, model);
        } catch (IOException e) {
            throw new MavenExecutionException(e.getMessage(), e);
        }

        mavenProject.setFile(newPom);
    }

    private static File getNewPomFile(MavenProject project, PluginConfig pluginConfig) throws MavenExecutionException {
        try {
            if (pluginConfig.shouldGenerateTemporaryFile) {
                return File.createTempFile("pom", ".verinf");
            } else {
                return new File(project.getBasedir(), "pom.xml.new-version");
            }
        } catch (IOException e) {
            throw new MavenExecutionException(e.getMessage(), e);
        }
    }

    // private void updateProjectx(MavenProject project) throws MavenExecutionException {
    //
    //     // if (Util.isDisabled()) {
    //     //     if (initialized.compareAndSet(false, true)) {
    //     //         logger.debug(String.format("%s is disabled", getClass().getSimpleName()));
    //     //     }
    //     //     return;
    //     // }
    //
    //     // Get the plugin config
    //     PluginConfig pluginConfig = pluginConfigProvider.getForProject(project);
    //     if (pluginConfig == null) {
    //         return;
    //     }
    //
    //     // Store the old version before changing it
    //     String oldVersion = project.getVersion();
    //
    //     // Now use the strategy to figure out the new version
    //     String newVersion = getNewVersion(pluginConfig.versionStrategy, project);
    //
    //     log.info("Inferred project version: " + newVersion);
    //
    //     Model originalModel = project.getModel();
    //     Path originalPomFile = originalModel.getPomFile().toPath().toAbsolutePath();
    //     Path gittlePomFile = originalPomFile.resolveSibling(GITTLE_POM_XML);
    //     try {
    //         Model gittleModel = ExtensionUtil.readModelFromPom(originalPomFile);
    //         ExtensionUtil.copyVersions(originalModel, gittleModel);
    //         // Now write the updated model out to a file so we can point the project to it.
    //         ExtensionUtil.writeModelToPom(gittleModel, gittlePomFile);
    //         project.setPomFile(gittlePomFile.toFile());
    //         log.debug("Updated project with newly generated gittle pom " + gittlePomFile);
    //     } catch (Exception e) {
    //         log.error("Failed creating new gittle pom at " + gittlePomFile, e);
    //     }
    // }
}
