package org.emergent.maven.gitver.core;

import lombok.NonNull;
import lombok.Value;

@Value
@lombok.Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class ArtifactCoordinates {

  @NonNull String groupId;
  @NonNull String artifactId;
  @NonNull String version;

  @Override
  public String toString() {
    return String.join(":", groupId, artifactId, version);
  }
}
