package org.emergent.maven.gitver.core.version;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;
import lombok.Value;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class SemVerTest {

  @Test
  void mutatingSingulars() {
    SemVer semVer1 = SemVer.zero();
    SemVer semVer2 = semVer1.withPrerelease("SNAPSHOT").withBuild("f0e1d2");
    assertThat(semVer1).asString().isEqualTo("0.0.0");
    assertThat(semVer2).asString().isEqualTo("0.0.0-SNAPSHOT+f0e1d2");
  }

  @Test
  @DisplayName("Default SemVer version value")
  void defaultVersion() {
    assertThat(SemVer.zero())
      .as("Semver Default")
      .extracting(SemVer::getMajor, SemVer::getMinor, SemVer::getPatch)
      .containsExactly(0, 0, 0);
  }

  @Test
  @DisplayName("Core version components extracted")
  void coreSemVerComponents() {
    assertThat(semver())
      .as("Semver 1.2.3")
      .extracting(SemVer::getMajor, SemVer::getMinor, SemVer::getPatch)
      .containsExactly(1, 2, 3);
  }

  @Test
  @DisplayName("Core version to String")
  void coreSemVerToString() {
    assertThat(semver()).as("Semver 1.2.3").asString().isEqualTo("1.2.3");
  }

  @ParameterizedTest
  @CsvSource({"-1,1,1,-1", "2,-2,2,-2", "3,3,-3,-3"})
  @DisplayName("Negative version component value")
  void negativeVersionTest(int major, int minor, int patch, int negativeValue) {
    Throwable exception =
      Assertions.catchThrowable(
        () -> SemVer.builder().setMajor(major).setMinor(minor).setPatch(patch).build());
    assertThat(exception)
      .isNotNull()
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage(String.format("Number %s must be a non-negative integer", negativeValue));
  }

  @Test
  void validate() {}

  @Test
  void withVersions() {
    assertThat(semver()).as("Semver 1.2.3").asString().isEqualTo("1.2.3");
  }

  @Test
  @DisplayName("Null prerelease indicator")
  void withNullPrereleaseIndicator() {
    Throwable npe =
      catchThrowable(() -> semver().withPrerelease(null));
    assertThat(npe).isNotNull()
      .isInstanceOf(NullPointerException.class)
      .hasMessage("Identifier must not be null");
  }

  @Test
  @DisplayName("Single prerelease identifier with allowed characters")
  void withSinglePrereleaseIdentifier() {
    assertThat(semver().withPrerelease("Al-pha01"))
      .as("Prerelease Version with all allowed characters")
      .asString()
      .isEqualTo("1.2.3-Al-pha01");
  }

  @Test
  @DisplayName("New Prerelease identifier removing existing")
  void withNewPrereleaseIdentifierOverridingPrevious() {
    assertThat(semver().withPrerelease("Alpha-01").withNewPrerelease("Alpha-New-01"))
      .as("New prerelease version overriding previous")
      .asString()
      .isEqualTo("1.2.3-Alpha-New-01");
  }

  @Test
  @DisplayName("Numeric prerelease identifier without leading zeros")
  void withNumericWithoutLeadingZerosPrereleaseIdentifier() {
    assertThat(semver().withPrerelease("1000234"))
      .as("Prerelease Version with all allowed characters")
      .asString()
      .isEqualTo("1.2.3-1000234");
  }

  @Test
  @DisplayName("Numeric prerelease identifier with leading zeros")
  void withNumericWithLeadingZerosPrereleaseIdentifier() {
    Throwable ex = catchThrowable(() -> semver().withPrerelease("0001234"));
    assertThat(ex)
      .isNotNull()
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Leading zeros are not allowed for numerical identifier '0001234'");
  }

  @Test
  @DisplayName("Prerelease indicator")
  void isPrerelease() {
    assertThat(semver().withPrerelease("Al-pha01"))
      .as("Prerelease Version with all allowed characters")
      .extracting(SemVer::isPrerelease)
      .isEqualTo(true);
  }

  @Test
  @DisplayName("Not Prerelease indicator")
  void isNotPrerelease() {
    assertThat(semver().isPrerelease()).as("Not a prerelease").isFalse();
  }

  @Test
  @DisplayName("Single prerelease identifier with disallowed characters")
  void withPrereleaseInvalidIdentifier() {
    Throwable exception = catchThrowable(() -> semver().withPrerelease("Al-pha01#"));
    assertThat(exception)
      .isNotNull()
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Identifier 'Al-pha01#' does not match pattern '^[0-9A-Za-z-]+$'");
  }

  @Test
  @DisplayName("Multiple prerelease identifiers")
  void withMultiplePrereleaseIdentifier() {
    assertThat(semver().withPrerelease("alpha").withPrerelease("1").withPrerelease("2"))
      .as("Prerelease Version")
      .asString()
      .isEqualTo("1.2.3-alpha.1.2");
  }

  @Test
  @DisplayName("Is initial development version")
  void isInitialDevelopment() {
    assertThat(Arrays.asList(SemVer.zero(), SemVer.builder().setMinor(2).setPatch(3).build()))
      .as("Semver Zero Version")
      .allSatisfy(semVer -> assertThat(semVer.isInitialDevelopment()).isTrue());
  }

  @Test
  @DisplayName("Is not initial development version")
  void isNotInitialDevelopment() {
    assertThat(semver().isInitialDevelopment()).as("Semver non-Zero Version").isFalse();
  }

  @Test
  @DisplayName("Single Build identifier with allowed characters")
  void withSingleBuildIdentifier() {
    assertThat(semver().withBuild("Some-01"))
      .as("Build Version with all allowed characters")
      .asString()
      .isEqualTo("1.2.3+Some-01");
  }

  @Test
  @DisplayName("New Build identifier removing existing")
  void withNewBuildIdentifierOverridingPrevious() {
    assertThat(semver().withBuild("Some-01").withNewBuild("Some-New-01"))
      .as("New build version overriding previous")
      .asString()
      .isEqualTo("1.2.3+Some-New-01");
  }

  @Test
  @DisplayName("Build Identifier with prerelease identifier")
  void buildWithPrereleaseIdentifier() {
    assertThat(semver().withPrerelease("Alpha-01").withBuild("Some-01"))
      .as("Build Identifier with prerelease identifier")
      .asString()
      .isEqualTo("1.2.3-Alpha-01+Some-01");
  }

  @Test
  @DisplayName("Multiple build and prerelease identifiers")
  void multipleBuildWithPrereleaseIdentifier() {
    assertThat(
      semver().withPrerelease("Alpha").withPrerelease("1").withBuild("Some").withBuild("001"))
      .as("Multiple build and prerelease identifiers")
      .asString()
      .isEqualTo("1.2.3-Alpha.1+Some.001");
  }

  @Test
  @DisplayName("Single Build identifier with disallowed characters")
  void withBuildInvalidIdentifier() {
    Throwable exception = catchThrowable(() -> semver().withBuild("Some-01#"));
    assertThat(exception)
      .isNotNull()
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Identifier 'Some-01#' does not match pattern '^[0-9A-Za-z-]+$'");
  }

  @Test
  @DisplayName("SemVer Zero")
  void zero() {
    assertThat(SemVer.zero()).asString().isEqualTo("0.0.0");
  }

  @ParameterizedTest
  @DisplayName("Parameterized version increment")
  @MethodSource("versionProvider")
  void verifyVersionIncrement(Wrapped process, SemVer semVer, String toStringValue) {
    assertThat(process.getProcessor().apply(semVer)).asString().isEqualTo(toStringValue);
  }

  public static Stream<Arguments> versionProvider() {
    return Stream.of(
      Arguments.of(new Wrapped(SemVer::incrementMajor, "incrementMajor"), semver(), "2.0.0"),
      Arguments.of(new Wrapped(SemVer::incrementMinor, "incrementMinor"), semver(), "1.3.0"),
      Arguments.of(new Wrapped(SemVer::incrementPatch, "incrementPatch"), semver(), "1.2.4"),
      Arguments.of(
        new Wrapped(SemVer::incrementMajor, "incrementMajor with build"),
        semver().withBuild("hash1"),
        "2.0.0"),
      Arguments.of(
        new Wrapped(SemVer::incrementMinor, "incrementMinor with build"),
        semver().withBuild("hash1"),
        "1.3.0"),
      Arguments.of(
        new Wrapped(SemVer::incrementPatch, "incrementPatch with build"),
        semver().withBuild("hash1"),
        "1.2.4"),
      Arguments.of(
        new Wrapped(SemVer::incrementMajor, "incrementMajor with prerelease"),
        semver().withPrerelease("alpha1"),
        "2.0.0"),
      Arguments.of(
        new Wrapped(SemVer::incrementMinor, "incrementMinor with prerelease"),
        semver().withPrerelease("alpha1"),
        "1.3.0"),
      Arguments.of(
        new Wrapped(SemVer::incrementPatch, "incrementPatch with prerelease"),
        semver().withPrerelease("alpha1"),
        "1.2.4"));
  }

  private static SemVer semver() {
    return semver(1, 2, 3);
  }

  private static SemVer semver(int major, int minor, int patch) {
    return SemVer.of(major, minor, patch);
  }

  @Value
  public static class Wrapped {
    Function<SemVer, SemVer> processor;
    String methodName;

    @Override
    public String toString() {
      return methodName;
    }
  }
}
