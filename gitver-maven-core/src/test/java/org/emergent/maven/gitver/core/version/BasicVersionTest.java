package org.emergent.maven.gitver.core.version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.Arrays;
import java.util.stream.Stream;
import lombok.Value;
import org.assertj.core.api.Assertions;
import org.emergent.maven.gitver.core.version.BasicVersion.Builder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class BasicVersionTest {

    @Test
    @DisplayName("Default SimpleVer version value")
    void defaultVersion() {
        assertThat(BasicVersion.zero())
                .as("Semver Default")
                .extracting(
                        BasicVersion::getMajor, BasicVersion::getMinor, BasicVersion::getPatch, BasicVersion::getCommit)
                .containsExactly(0, 0, 0, 0);
    }

    @Test
    @DisplayName("Core version components extracted")
    void coreSimpleVerComponents() {
        assertThat(semver().setCommit(4).build())
                .as("Semver 1.2.3-4")
                .extracting(
                        BasicVersion::getMajor, BasicVersion::getMinor, BasicVersion::getPatch, BasicVersion::getCommit)
                .containsExactly(1, 2, 3, 4);
    }

    @Test
    @DisplayName("Core version to String")
    void coreSimpleVerToString() {
        assertThat(semver().build()).as("Semver 1.2.3").asString().isEqualTo("1.2.3");
    }

    @ParameterizedTest
    @CsvSource({"-1,1,1,-1", "2,-2,2,-2", "3,3,-3,-3"})
    @DisplayName("Negative version component value")
    void negativeVersionTest(int major, int minor, int patch, int negativeValue) {
        Throwable exception = Assertions.catchThrowable(() -> BasicVersion.builder()
                .setMajor(major)
                .setMinor(minor)
                .setPatch(patch)
                .build());
        assertThat(exception)
                .isNotNull()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(" must be a non-negative integer");
    }

    @Test
    public void setInitialValues() {
        BasicVersion config =
                BasicVersion.builder().setMajor(1).setMinor(2).setPatch(3).build();
        assertThat(config).extracting("major", "minor", "patch").containsExactly(1, 2, 3);
    }

    @Test
    void validate() {}

    @Test
    void withVersions() {
        assertThat(semver().build()).as("1.2.3").asString().isEqualTo("1.2.3");
    }

    @Test
    @DisplayName("Is initial development version")
    void isInitialDevelopment() {
        assertThat(Arrays.asList(
                        BasicVersion.zero(),
                        BasicVersion.builder().setMinor(2).setPatch(3).build()))
                .as("Semver Zero Version")
                .allSatisfy(semVer -> assertThat(semVer.isInitialDevelopment()).isTrue());
    }

    @Test
    @DisplayName("Is not initial development version")
    void isNotInitialDevelopment() {
        assertThat(semver().build().isInitialDevelopment())
                .as("Semver non-Zero Version")
                .isFalse();
    }

    @Test
    @DisplayName("SimpleVer Zero")
    void zero() {
        assertThat(BasicVersion.zero()).asString().isEqualTo("0.0.0");
    }

    @Test
    public void setInitialMajor_negative() {
        Throwable ex = catchThrowable(() -> BasicVersion.builder().setMajor(-1).build());
        assertThat(ex)
                .isNotNull()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gv.initial.major must be a non-negative integer");
    }

    @Test
    public void setInitialMinor_negative() {
        Throwable ex = catchThrowable(() -> BasicVersion.builder().setMinor(-1).build());
        assertThat(ex)
                .isNotNull()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gv.initial.minor must be a non-negative integer");
    }

    @Test
    public void setInitialPatch_negative() {
        Throwable ex = catchThrowable(() -> BasicVersion.builder().setPatch(-1).build());
        assertThat(ex)
                .isNotNull()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("gv.initial.patch must be a non-negative integer");
    }

    @ParameterizedTest
    @DisplayName("Parameterized version increment")
    @MethodSource("versionProvider")
    void verifyVersionIncrement(Wrapped process, Builder semVer, String toStringValue) {
        assertThat(semVer.increment(process.getIncrementType()).build())
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

    private static Builder semver() {
        return semver(1, 2, 3);
    }

    @SuppressWarnings("SameParameterValue")
    private static Builder semver(int major, int minor, int patch) {
        return BasicVersion.builder().setMajor(major).setMinor(minor).setPatch(patch);
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
