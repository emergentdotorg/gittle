package org.emergent.gittle.core.util;

import java.util.Objects;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Parent;
import org.apache.maven.project.MavenProject;

public class GroupArtifactVersion {

    public final String groupId;
    public final String artifactId;
    public final String version;

    GroupArtifactVersion(String groupId, String artifactId, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
    }

    public static GroupArtifactVersion of(String groupId, String artifactId, String version) {
        return new GroupArtifactVersion(groupId, artifactId, version);
    }

    public static GroupArtifactVersion fromMavenProject(MavenProject mavenProject) {
        return from(mavenProject);
    }

    public static GroupArtifactVersion from(MavenProject mavenProject) {
        return GroupArtifactVersion.of(
                mavenProject.getGroupId(),
                mavenProject.getArtifactId(),
                mavenProject.getVersion()
        );
    }

    public static GroupArtifactVersion from(Parent parent) {
        return GroupArtifactVersion.of(
                parent.getGroupId(),
                parent.getArtifactId(),
                parent.getVersion()
        );
    }

    public static GroupArtifactVersion from(Dependency dependency) {
        return GroupArtifactVersion.of(
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getVersion()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupArtifactVersion)) return false;
        GroupArtifactVersion that = (GroupArtifactVersion) o;
        boolean groupIdEquals = groupId == null ? that.groupId == null : groupId.equalsIgnoreCase(that.groupId);
        boolean artifactIdEquals =
                artifactId == null ? that.artifactId == null : artifactId.equalsIgnoreCase(that.artifactId);
        boolean versionEquals = version == null ? that.version == null : version.equalsIgnoreCase(that.version);
        return groupIdEquals && artifactIdEquals && versionEquals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }
}
