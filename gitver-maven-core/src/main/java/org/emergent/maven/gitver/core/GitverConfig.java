package org.emergent.maven.gitver.core;

import lombok.AccessLevel;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.experimental.SuperBuilder;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.emergent.maven.gitver.core.Constants.RELEASE_BRANCHES_DEF;
import static org.emergent.maven.gitver.core.Constants.TAG_PATTERN_DEF;
import static org.emergent.maven.gitver.core.Constants.VERSION_PATTERN_DEF;

@Value
@NonFinal
@lombok.experimental.FieldDefaults(level = AccessLevel.PROTECTED)
@lombok.experimental.Accessors(fluent = false)
@SuperBuilder(toBuilder = true)
public class GitverConfig implements PropCodec.Codable {

    @lombok.Builder.Default
    String newVersion = "";

    @lombok.Builder.Default
    String releaseBranches = RELEASE_BRANCHES_DEF;

    @lombok.Builder.Default
    String tagNamePattern = TAG_PATTERN_DEF;

    @lombok.Builder.Default
    String versionPattern = VERSION_PATTERN_DEF;

    public Map<String, String> asMap() {
        return PropCodec.toProperties(this);
    }

    public Set<String> getReleaseBranchesSet() {
        String branchesString = Optional.ofNullable(releaseBranches).orElse(RELEASE_BRANCHES_DEF);
        return Arrays.stream(branchesString.split(","))
                .map(String::trim).collect(Collectors.toCollection(TreeSet::new));
    }

    @SuppressWarnings("unchecked")
    public static <T> T acquireDefault(Class<?> type) {
//        return gsonUtil.rebuild(Map.of(), src.getClass());
        try {
            Object builder = type.getDeclaredMethod("builder").invoke(null);
            Class<?> builderClass = builder.getClass();
            Method buildMethod = builderClass.getMethod("build");
            return (T)buildMethod.invoke(builder);
        } catch (ReflectiveOperationException e) {
            throw new GitverException(e.getMessage(), e);
        }
    }

}
