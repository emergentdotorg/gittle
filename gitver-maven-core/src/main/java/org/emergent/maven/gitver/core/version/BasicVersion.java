package org.emergent.maven.gitver.core.version;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Tolerate;
import org.emergent.maven.gitver.core.Constants;
import org.emergent.maven.gitver.core.Mapper;
import org.emergent.maven.gitver.core.Util;

@Value
@Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class BasicVersion implements Comparable<BasicVersion> {

  private static final BasicVersion ZERO = BasicVersion.builder().build();

  public static final Comparator<BasicVersion> COMPARATOR = Comparator.comparing(BasicVersion::getMajor)
    .thenComparing(BasicVersion::getMinor).thenComparing(BasicVersion::getPatch).thenComparing(BasicVersion::getCommit);

  int major;
  int minor;
  int patch;
  int commit;

  private BasicVersion(int major, int minor, int patch, int commit) {
    this.major = Util.assertNotNegative(major, Constants.GV_INITIAL_MAJOR);
    this.minor = Util.assertNotNegative(minor, Constants.GV_INITIAL_MINOR);
    this.patch = Util.assertNotNegative(patch, Constants.GV_INITIAL_PATCH);
    this.commit = Util.assertNotNegative(commit);
  }

  public static BasicVersion zero() {
    return ZERO;
  }

  public static BasicVersion from(String prefix, Properties props) {
    return BasicVersion.builder()
      .setMajor(props.getProperty(prefix + "major"))
      .setMinor(props.getProperty(prefix + "minor"))
      .setPatch(props.getProperty(prefix + "patch"))
      .setCommit(props.getProperty(prefix + "commitNumber"))
      .build();
  }

  public boolean isZero() {
    return major == 0 && minor == 0 && patch == 0 && commit == 0;
  }

  public boolean isNonZero() {
    return !isZero();
  }

  public boolean isInitialDevelopment() {
    return getMajor() == 0;
  }

  public Map<String, String> toProperties(String prefix) {
    Mapper m = Mapper.create(prefix)
      .put("major", major)
      .put("minor", minor)
      .put("patch", patch)
      .put("commitNumber", commit);
    return m.toMap();
  }

  @Override
  public int compareTo(BasicVersion o) {
    return COMPARATOR.compare(this, o);
  }

  @Override
  public String toString() {
    return Stream.of(getMajor(), getMinor(), getPatch())
      .map(String::valueOf)
      .collect(Collectors.joining("."))
      + (commit == 0 ? "" : "-" + commit);
  }

  @SuppressWarnings("UnusedReturnValue")
  public static class Builder {

    @Tolerate
    public Builder setMajor(String value) {
      return setMajor(parseInt(value));
    }

    @Tolerate
    public Builder setMinor(String value) {
      return setMinor(parseInt(value));
    }

    @Tolerate
    public Builder setPatch(String value) {
      return setPatch(parseInt(value));
    }

    @Tolerate
    public Builder setCommit(String value) {
      return setCommit(parseInt(value));
    }

    public Builder reset(BasicVersion sv) {
      setMajor(sv.getMajor());
      setMinor(sv.getMinor());
      setPatch(sv.getPatch());
      setCommit(sv.getCommit());
      return this;
    }

    public Builder increment(VersionIncrementType increment) {
      switch (increment) {
        case MAJOR -> setMajor(major + 1).setMinor(0).setPatch(0).setCommit(0);
        case MINOR -> setMinor(minor + 1).setPatch(0).setCommit(0);
        case PATCH -> setPatch(patch + 1).setCommit(0);
        case COMMIT -> setCommit(commit + 1);
      }
      return this;
    }

    private static int parseInt(String value) {
      return Optional.ofNullable(value).filter(Util::isNotEmpty).map(Integer::parseInt).orElse(0);
    }
  }
}
