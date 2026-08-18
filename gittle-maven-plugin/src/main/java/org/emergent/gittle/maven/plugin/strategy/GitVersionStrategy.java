package org.emergent.gittle.maven.plugin.strategy;

import com.github.zafarkhaja.semver.Version;
import fr.brouillard.oss.jgitver.GitVersionCalculator;
import fr.brouillard.oss.jgitver.metadata.Metadatas;
import java.io.File;
import java.util.Optional;
import java.util.Properties;
import javax.inject.Named;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.emergent.gittle.maven.plugin.VersionException;

@Slf4j
@Setter
@Named("git")
public class GitVersionStrategy implements VersionStrategy {

    public static final String DEFAULT_NON_QUALIFIED_BRANCH = "master";
    public static final String DEFAULT_PRE_RELEASE_STAGE = "dev";
    public static final String DEFAULT_DIRTY_QUALIFIER = "uncommitted";
    public static final String PROPERTY_PREFIX = "project.";
    public static final String NORMAL_VERSION_PROPERTY = PROPERTY_PREFIX + "normalVersion";
    public static final String PRE_RELEASE_VERSION_PROPERTY = PROPERTY_PREFIX + "preReleaseVersion";
    public static final String BUILD_METADATA_PROPERTY = PROPERTY_PREFIX + "buildMetadata";
    public static final String DOCKER_SAFE_VERSION_PROPERTY = PROPERTY_PREFIX + "dockerSafeVersion";
    public static final String FULL_INFERRED_VERSION_PROPERTY = PROPERTY_PREFIX + "fullInferredVersion";

    @Parameter(name = "nonQualifierBranches", defaultValue =  DEFAULT_NON_QUALIFIED_BRANCH)
    private String nonQualifierBranches;

    @Parameter(name = "preReleaseStage", defaultValue = DEFAULT_PRE_RELEASE_STAGE)
    private String preReleaseStage;

    @Parameter(name = "dirtyQualifier", defaultValue = DEFAULT_DIRTY_QUALIFIER)
    private String dirtyQualifier;

    @Parameter(name = "snapshot", defaultValue = "false")
    private Boolean snapshot;

    @Override
    public String getVersion(MavenProject mavenProject) throws VersionException {
        File rootDir = mavenProject.getBasedir();
        try (GitVersionCalculator calculator = GitVersionCalculator.location(rootDir)) {
            Version semVer = getVersionInternal(calculator);
            setProjectProperties(mavenProject.getProperties(), semVer);
            return semVer.toString();
        } catch (Exception e) {
            throw new VersionException("Cannot close GitVersionCalculator object for project: " + rootDir, e);
        }
    }

    protected void setProjectProperties(Properties properties, Version semVer) {
        properties.setProperty(NORMAL_VERSION_PROPERTY, semVer.getNormalVersion());
        properties.setProperty(PRE_RELEASE_VERSION_PROPERTY, semVer.getPreReleaseVersion());
        properties.setProperty(BUILD_METADATA_PROPERTY, semVer.getBuildMetadata());
        properties.setProperty(FULL_INFERRED_VERSION_PROPERTY, semVer.toString());
        properties.setProperty(DOCKER_SAFE_VERSION_PROPERTY, semVer.toString().replace('+', '-'));
    }

    // This method exists solely to facilitate easier unit testing
    protected Version getVersionInternal(GitVersionCalculator calculator) {
        configureGitVersionCalculator(calculator);

        fr.brouillard.oss.jgitver.Version calculatedVersion = calculator.getVersionObject();

        Version normalVersion = Version.forIntegers(
                calculatedVersion.getMajor(),
                calculatedVersion.getMinor(),
                calculatedVersion.getPatch()
        );

        Version.Builder semVerBuilder = new Version.Builder()
                .setNormalVersion(normalVersion.toString());

        int commitDistance = calculator.meta(Metadatas.COMMIT_DISTANCE)
                .map(Integer::valueOf)
                .orElse(0);

        StringBuilder preReleaseVersion = new StringBuilder();

        // SNAPSHOT builds don't care if repo is dirty, so we don't have to go any further.
        if (snapshot != null && snapshot && commitDistance > 0) {
            preReleaseVersion.append("SNAPSHOT");
            semVerBuilder.setPreReleaseVersion(preReleaseVersion.toString());
            return semVerBuilder.build();
        }

        preReleaseVersion.append(preReleaseStage).append(".").append(commitDistance);

        boolean isDirty = calculator.meta(Metadatas.DIRTY)
                .map(Boolean::valueOf)
                .orElse(false);

        if (isDirty) {
            preReleaseVersion.append(".").append(dirtyQualifier);
        }

        if (isDirty || commitDistance > 0) {
            semVerBuilder.setPreReleaseVersion(preReleaseVersion.toString());
            calculator.meta(Metadatas.GIT_SHA1_8).ifPresent(semVerBuilder::setBuildMetadata);
        }

        return semVerBuilder.build();
    }

    private void configureGitVersionCalculator(GitVersionCalculator calculator) {
        calculator.setNonQualifierBranches(Optional.ofNullable(nonQualifierBranches)
                .orElse(DEFAULT_NON_QUALIFIED_BRANCH));
        // calculator.setStrategy(Strategies.MAVEN);
        // calculator.setUseDistance(true);
        // calculator.setUseGitCommitId(true);
        // calculator.setAutoIncrementPatch(true);
    }
}
