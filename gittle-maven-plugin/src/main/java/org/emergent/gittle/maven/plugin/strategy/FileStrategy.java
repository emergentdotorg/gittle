package org.emergent.gittle.maven.plugin.strategy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.inject.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.emergent.gittle.maven.plugin.VersionException;

/**
 * Strategy which reads a version string from a 'VERSION' file which  contains a single version string such as '1.2.3'.
 */
@Slf4j
@Named("file")
public class FileStrategy implements VersionStrategy {

    @Parameter(name = "versionFilePath", defaultValue = "VERSION")
    private String versionFilePath;

    @Override
    public String getVersion(MavenProject mavenProject) throws VersionException {
        String versionString;

        File versionFile = new File(mavenProject.getBasedir(), versionFilePath);
        try (BufferedReader reader = new BufferedReader(new FileReader(versionFile))) {
            // just return the first line of the file, any other format is NOT supported.
            versionString = reader.readLine();
        } catch (IOException e) {
            throw new VersionException(
                    "Failed to read version file: [" + versionFile.getAbsolutePath() + "]",
                    e
            );
        }

        return versionString;
    }
}
