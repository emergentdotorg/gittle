package org.emergent.maven.gitver.core.version;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.Singular;
import lombok.Value;
import lombok.experimental.Tolerate;
import org.emergent.maven.gitver.core.Util;

import static java.lang.String.format;

/**
 * Represents a <a href="https://semver.org/spec/v2.0.0.html">semantic version</a>.
 */
@Value
@lombok.Builder(toBuilder = true, setterPrefix = "set", builderClassName = "Builder")
public class SemVer implements Comparable<SemVer> {

  private static final SemVer ZERO = SemVer.builder().build();

  @Default
  int major = 0;
  @Default
  int minor = 0;
  @Default
  int patch = 0;
  @Singular
  List<Prerelease> prereleases;
  @Singular
  List<BuildMeta> buildmetas;

  private SemVer(
    int major, int minor, int patch, List<Prerelease> prereleases, List<BuildMeta> buildmetas) {
    this.major = Util.assertNotNegative(major);
    this.minor = Util.assertNotNegative(minor);
    this.patch = Util.assertNotNegative(patch);
    this.prereleases = prereleases;
    this.buildmetas = buildmetas;
  }

  public static Optional<SemVer> fromString(String version) {
    Matcher matcher = Util.VERSION_REGEX2.matcher(version);
    if (!matcher.matches()) {
      return Optional.empty();
    }
    String prerelease = matcher.group("prerelease");
    List<Prerelease> prereleaseArr = Arrays.stream(prerelease.split("\\."))
      .map(Prerelease::new).toList();
    String buildmeta = matcher.group("buildmeta");
    List<BuildMeta> buildmetaArr = Arrays.stream(buildmeta.split("\\."))
      .map(BuildMeta::new).toList();

    return Optional.of(SemVer.builder()
      .setMajor(Integer.parseInt(matcher.group("major")))
      .setMinor(Integer.parseInt(matcher.group("major")))
      .setPatch(Integer.parseInt(matcher.group("major")))
      .setPrereleases(prereleaseArr)
      .setBuildmetas(buildmetaArr)
      .build());
  }

  public static SemVer of(int major, int minor, int patch) {
    return builder().setMajor(major).setMinor(minor).setPatch(patch).build();
  }

  public static SemVer zero() {
    return ZERO;
  }

  public SemVer withPrerelease(String prerelease) {
    return toBuilder().setPrerelease(prerelease).build();
  }

  public SemVer withNewPrerelease(String prerelease) {
    return toBuilder().clearPrereleases().setPrerelease(prerelease).build();
  }

  public SemVer withBuild(String build) {
    return toBuilder().setBuildMeta(build).build();
  }

  public SemVer withNewBuild(String build) {
    return toBuilder().clearBuildmetas().setBuildMeta(build).build();
  }

  public SemVer incrementMajor() {
    return this.toBuilder()
      .setMajor(major + 1)
      .setMinor(0)
      .setPatch(0)
      .clearPrereleases()
      .clearBuildmetas()
      .build();
  }

  public SemVer incrementMinor() {
    return this.toBuilder()
      .setMinor(minor + 1)
      .setPatch(0)
      .clearPrereleases()
      .clearBuildmetas()
      .build();
  }

  public SemVer incrementPatch() {
    return this.toBuilder().setPatch(patch + 1).clearPrereleases().clearBuildmetas().build();
  }

  public boolean isInitialDevelopment() {
    return getMajor() == 0;
  }

  public boolean isPrerelease() {
    return !this.prereleases.isEmpty();
  }

  @Override
  public String toString() {
    return Stream.of(getMajor(), getMinor(), getPatch())
      .map(String::valueOf)
      .collect(Collectors.joining("."))
      + prereleases.stream()
      .map(Prerelease::getLabel)
      .collect(
        () -> new StringJoiner(".", "-", "").setEmptyValue(""),
        StringJoiner::add,
        StringJoiner::merge)
      + buildmetas.stream()
      .map(BuildMeta::getLabel)
      .collect(
        () -> new StringJoiner(".", "+", "").setEmptyValue(""),
        StringJoiner::add,
        StringJoiner::merge);
  }

  @Override
  public int compareTo(SemVer o) {
    int result =
      Comparator.comparing(SemVer::getMajor)
        .thenComparing(SemVer::getMinor)
        .thenComparing(SemVer::getPatch)
        .compare(this, o);
    if (result != 0) {
      return result;
    }

    List<Prerelease> pcol1 = getPrereleases();
    List<Prerelease> pcol2 = o.getPrereleases();

    int pcolCnt = Math.min(pcol1.size(), pcol2.size());
    for (int i = 0; i < pcolCnt; i++) {
      Prerelease p1 = pcol1.get(i);
      Prerelease p2 = pcol2.get(i);
      result = Comparator.<Prerelease>nullsFirst(Comparator.naturalOrder()).compare(p1, p2);
      if (result != 0) {
        return result;
      }
    }

    return pcol1.size() - pcol2.size();
  }

  @Getter
  public static class Identifier {

    static Pattern PATTERN = Pattern.compile("^[0-9A-Za-z-]+$");
    static Pattern NUMERIC_VALUE = Pattern.compile("^[0-9]*$");
    private final String label;

    protected Identifier(String label) {
      this.label = validated(label);
    }

    @Override
    public String toString() {
      return getLabel();
    }

    static String validated(String label) {
      Objects.requireNonNull(label, "Identifier must not be null");
      return Util.mustPass(
        label,
        v -> PATTERN.matcher(v).matches(),
        () -> format("Identifier '%s' does not match pattern '%s'", label, PATTERN.pattern()));
    }
  }

  /**
   * SemVer <a href="https://semver.org/spec/v2.0.0.html#spec-item-9">Prerelease Identifier</a>
   */
  public static class Prerelease extends Identifier implements Comparable<Prerelease> {
    private Prerelease(String label) {
      super(validated(label));
    }

    public static Prerelease of(String label) {
      return new Prerelease(label);
    }

    public boolean isNumeric() {
      return isNumeric(getLabel());
    }

    @Override
    public int compareTo(Prerelease o) {
      String l1 = getLabel();
      String l2 = o.getLabel();
      boolean numeric = isNumeric(l1);
      if (numeric != isNumeric(l2)) {
        return numeric ? -1 : 1;
      } else if (numeric) {
        return Integer.compare(Integer.parseInt(l1), Integer.parseInt(l2));
      } else {
        return Comparator.<String>naturalOrder().compare(l1, l2);
      }
    }

    private static boolean isNumeric(String value) {
      return NUMERIC_VALUE.matcher(value).matches();
    }

    static String validated(String label) {
      Identifier.validated(label);
      return Util.mustPass(
        label,
        v -> !(NUMERIC_VALUE.matcher(v).matches() && v.startsWith("0")),
        () -> format("Leading zeros are not allowed for numerical identifier '%s'", label));
    }
  }

  /**
   * SemVer <a href="https://semver.org/spec/v2.0.0.html#spec-item-10">Build Metadata</a>
   */
  public static class BuildMeta extends Identifier {
    private BuildMeta(String label) {
      super(label);
    }

    public static BuildMeta of(String label) {
      return new BuildMeta(label);
    }
  }

  @SuppressWarnings("unused")
  public static class Builder {

    @Tolerate
    public Builder setPrerelease(String... value) {
      return setPrereleases(Arrays.stream(value).map(Prerelease::of).collect(Collectors.toList()));
    }

    @Tolerate
    public Builder setBuildMeta(String... value) {
      return setBuildmetas(Arrays.stream(value).map(BuildMeta::of).collect(Collectors.toList()));
    }
  }
}
