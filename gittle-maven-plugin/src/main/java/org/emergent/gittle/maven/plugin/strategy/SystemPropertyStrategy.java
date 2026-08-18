package org.emergent.gittle.maven.plugin.strategy;

import javax.inject.Named;
import lombok.Setter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

/**
 * Loads version from the System Property 'external.version'.
 */
@Setter
@Named("sysprop")
public class SystemPropertyStrategy implements VersionStrategy {
    private static final String EXTERNAL_VERSION = "external.version";

    private static final String EXTERNAL_VERSION_QUALIFIER = "external.version-qualifier";

    @Override
    public String getVersion(MavenProject mavenProject) {
        String newVersion = System.getProperty(EXTERNAL_VERSION, mavenProject.getVersion());
        String qualifier = StringUtils.trim(System.getProperty(EXTERNAL_VERSION_QUALIFIER));
        if (StringUtils.isNotBlank(qualifier)) {
            // TODO: this needs to be cleaned up, the calling method will re-add the -SNAPSHOT if needed, but this is dirty
            newVersion = newVersion.replaceFirst("-SNAPSHOT", "") + "-" + qualifier;
        }

        return newVersion;
    }
}
