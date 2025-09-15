package org.emergent.maven.gitver.core.version;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Value;
import org.assertj.core.api.Assertions;
import org.emergent.maven.gitver.core.Util;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

public class StrategyFactoryTest {

    // @ParameterizedTest
    // @CsvSource({
    //     "Fix: [minor] corrected typo, .*\\[minor\\].*, true",
    //     "[minor] Fix: corrected typo, ^\\[minor\\].*, true",
    //     "Fix: [minor] corrected typo, ^\\[minor\\].*, false",
    //     "Update: improved performance, [minor], false"
    // })
    // public void testHasValueWithRegex(String commitMessage, String keyword, boolean expected) {
    //     KeywordsConfig versionConfig = KeywordsConfig.builder().setRegex(true).build();
    //
    //     Assertions.assertThat(StrategyFactory.hasValue(versionConfig, commitMessage, keyword))
    //             .isEqualTo(expected);
    // }
    //
    // @Test
    // public void testHasValueWithoutRegex() {
    //     KeywordsConfig versionConfig = KeywordsConfig.builder().setRegex(false).build();
    //
    //     String commitMessage = "Fix: [minor] corrected typo";
    //     assertThat(StrategyFactory.hasValue(versionConfig, commitMessage, "[minor]"))
    //             .isTrue();
    //
    //     commitMessage = "Update: improved performance";
    //     assertThat(StrategyFactory.hasValue(versionConfig, commitMessage, "[minor]"))
    //             .isFalse();
    // }

    @ParameterizedTest
    @CsvSource({"-1,1,1,-1", "2,-2,2,-2", "3,3,-3,-3"})
    @DisplayName("Negative version component value")
    void negativeVersionTest(int major, int minor, int patch, int negativeValue) {
        Throwable exception = Assertions.catchThrowable(() -> {
            Util.assertNotNegative(major);
            Util.assertNotNegative(minor);
            Util.assertNotNegative(patch);
        });
        assertThat(exception)
          .isNotNull()
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining(String.format("%s must be a non-negative integer", negativeValue));
    }

    @Disabled
    @ParameterizedTest
    @DisplayName("Parameterized version increment")
    @MethodSource("versionProvider")
    void verifyVersionIncrement(Wrapped process, String semVer, String toStringValue) {
        StringBuilder incremented = new StringBuilder(semVer);
        assertThat(incremented)
          .asString()
          .isEqualTo(toStringValue);
    }

    public static Stream<Arguments> versionProvider() {
        return Stream.of(
          Arguments.of(new Wrapped(VersionIncrementType.MAJOR, "incrementMajor"), semver(), "2.0.0"),
          Arguments.of(new Wrapped(VersionIncrementType.MINOR, "incrementMinor"), semver(), "1.3.0"),
          Arguments.of(new Wrapped(VersionIncrementType.PATCH, "incrementPatch"), semver(), "1.2.4"),
          Arguments.of(new Wrapped(VersionIncrementType.COMMIT, "incrementCommit"), semver(), "1.2.3-1"));
    }

    private static String semver() {
        return semver(1, 2, 3);
    }

    @SuppressWarnings("SameParameterValue")
    private static String semver(int major, int minor, int patch) {
        return Stream.of(major, minor, patch).map(String::valueOf).collect(Collectors.joining("."));
    }

    @Value
    public static class Wrapped {
        VersionIncrementType incrementType;
        String methodName;

        @Override
        public String toString() {
            return methodName;
        }
    }
}
