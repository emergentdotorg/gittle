package org.emergent.maven.gitver.plugin;

import java.util.Map;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.emergent.maven.gitver.core.Coordinates;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.version.VersionStrategy;

@Mojo(name = "props", defaultPhase = LifecyclePhase.INITIALIZE)
public class PropsMojo extends AbstractGitverMojo {

    @Override
    protected void execute0() {
        VersionStrategy strategy = getVersionStrategy();
        Map<String, String> properties = strategy.getPropertiesMap();
        getLog().info("Adding properties to project "
                + MessageUtils.buffer()
                        .mojo(Coordinates.builder()
                                .setGroupId(mavenProject.getGroupId())
                                .setArtifactId(mavenProject.getArtifactId())
                                .setVersion(mavenProject.getVersion())
                                .build())
                        .a(Util.join(properties)));
        mavenProject.getProperties().putAll(properties);
    }
}
