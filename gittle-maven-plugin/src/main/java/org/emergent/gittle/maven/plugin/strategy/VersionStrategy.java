package org.emergent.gittle.maven.plugin.strategy;

import org.apache.maven.project.MavenProject;
import org.emergent.gittle.maven.plugin.VersionException;

/**
 * Simple strategy for resolving a version.
 */
public interface VersionStrategy {

    String ROLE = VersionStrategy.class.getName();

    /**
     * Returns a new version based on some other source.
     *
     * @param mavenProject project which will be updated.
     * @return a new String version.
     * @throws VersionException thrown if there is any problems loading the new version.
     */
    String getVersion(MavenProject mavenProject) throws VersionException;
}
