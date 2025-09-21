package org.emergent.maven.gitver.plugin;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.emergent.maven.gitver.core.Coordinates;
import org.emergent.maven.gitver.core.GitverConfig;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.version.StrategyFactory;
import org.emergent.maven.gitver.core.version.VersionStrategy;

import static org.emergent.maven.gitver.core.Constants.NEW_VERSION;
import static org.emergent.maven.gitver.core.Constants.RELEASE_BRANCHES;
import static org.emergent.maven.gitver.core.Constants.TAG_NAME_PATTERN;
import static org.emergent.maven.gitver.core.Constants.VERSION_PATTERN;

@Getter
@Setter
@Mojo(name = PropsMojo.NAME, defaultPhase = LifecyclePhase.INITIALIZE)
public class PropsMojo extends AbstractGitverMojo {

    public static final String NAME = "props";

    @Parameter(name = NEW_VERSION,  property = NEW_VERSION_PROP)
    private String newVersion;

    @Parameter(name = RELEASE_BRANCHES, property = RELEASE_BRANCHES_PROP)
    private String releaseBranches;

    @Parameter(name = TAG_NAME_PATTERN, property = TAG_NAME_PATTERN_PROP)
    private String tagNamePattern;

    @Parameter(name = VERSION_PATTERN, property = VERSION_PATTERN_PROP)
    private String versionPattern;

    @Override
    protected void execute0() {
        VersionStrategy strategy = getVersionStrategy();
        Map<String, String> properties = strategy.asMap();
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

    protected VersionStrategy getVersionStrategy() {
        return StrategyFactory.getVersionStrategy(mavenProject.getBasedir(), getConfig());
    }

    @Override
    public GitverConfig getConfig() {
        Properties loaded = Util.loadProperties(mavenProject.getBasedir().toPath());
        of(newVersion).ifPresent(v -> loaded.setProperty(NEW_VERSION_PROP, v));
        of(releaseBranches).ifPresent(v -> loaded.setProperty(RELEASE_BRANCHES_PROP, v));
        of(tagNamePattern).ifPresent(v -> loaded.setProperty(TAG_NAME_PATTERN_PROP, v));
        of(versionPattern).ifPresent(v -> loaded.setProperty(VERSION_PATTERN_PROP, v));
        return GitverConfig.from(loaded);
    }

    private Optional<String> of(String value) {
        return Optional.ofNullable(value).filter(Util::isNotEmpty);
    }
}
