package org.emergent.gittle.core.old;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Tolerate;

@Value
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@lombok.experimental.FieldDefaults(level = AccessLevel.PRIVATE)
@lombok.experimental.Accessors(fluent = false)
@lombok.Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class Resolved {

  public static final String TAG_VERSION_DEF = "0.0.0";
  private static final String PREFIX = "gittle.resolved.";

  @lombok.Builder.Default
  String gitDir = null;
  @lombok.Builder.Default
  String branch = null;
  @NonNull
  @lombok.Builder.Default
  String hash = "";
  @lombok.Builder.Default
  String tagVersion = TAG_VERSION_DEF;
  @lombok.Builder.Default
  int commits = 0;
  @lombok.Builder.Default
  boolean dirty = false;

  public static class Builder {

    @Tolerate
    public Builder gitDir(String gitDir) {
      return setGitDir(gitDir);
    }

    @Tolerate
    public Builder branch(String branch) {
      return setBranch(branch);
    }

    @Tolerate
    public Builder hash(String hash) {
      return setHash(hash);
    }

    @Tolerate
    public Builder tagVersion(String tagged) {
      return setTagVersion(tagged);
    }

    @Tolerate
    public Builder commits(int commits) {
      return setCommits(commits);
    }

    @Tolerate
    public Builder dirty(boolean hash) {
      return setDirty(hash);
    }
  }
}
