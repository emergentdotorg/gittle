package org.emergent.maven.gitver.core;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.Value;

@Value
@lombok.Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class Coordinates {

    @NonNull
    String groupId;

    @NonNull
    String artifactId;

    @NonNull
    String version;

    @lombok.Builder.Default
    @NonNull
    String packaging = "";

    @lombok.Builder.Default
    @NonNull
    String classifier = "";

    @Override
    public String toString() {
        return Stream.of(groupId, artifactId, version, packaging, classifier)
                .filter(Util::isNotEmpty)
                .collect(Collectors.joining(":"));
    }

    public static class Builder {
        @Override
        public String toString() {
            return build().toString();
        }
    }
}
