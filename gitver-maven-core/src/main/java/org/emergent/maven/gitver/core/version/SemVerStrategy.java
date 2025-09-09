package org.emergent.maven.gitver.core.version;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.Setter;
import org.emergent.maven.gitver.core.GitVerConfig;
import org.emergent.maven.gitver.core.Util;

@Getter
public class SemVerStrategy implements VersionStrategy {

  private final AtomicInteger commitCount = new AtomicInteger(0);
  private final String branch;
  @Setter
  private String hash;
  private final SemVer semVer;
  private final GitVerConfig versionConfig;

  public SemVerStrategy(int major,
                        int minor,
                        int patch,
                        String branch,
                        String hash,
                        GitVerConfig versionConfig) {
    this(branch, hash, SemVer.of(major, minor, patch), versionConfig);
  }

  public SemVerStrategy(String branch, String hash, SemVer semVer, GitVerConfig versionConfig) {
    this.branch = branch;
    this.hash = hash;
    this.semVer = semVer;
    this.versionConfig = versionConfig;
  }

  @Override
  public String toVersionString() {
    return semVer.toString();
  }

  @Override
  public Map<String, String> toProperties() {
    Map<String, Object> properties = new TreeMap<>();
    properties.put(GITVER_VERSION_FULL, toVersionString());
    properties.putAll(getRefVersionData().toProperties());
    return Util.flatten(properties);
  }

  private RefVersionData getRefVersionData() {
    return RefVersionData.builder()
      .setBranch(branch)
      .setHash(hash)
      .setMajor(semVer.getMajor())
      .setMinor(semVer.getMinor())
      .setPatch(semVer.getPatch())
      .setCommit(commitCount.get())
      .build();
  }

  @Override
  public String toString() {
    return String.format("%s [branch: %s, version: %s, hash: %s]",
      getClass().getSimpleName(), branch, toVersionString(), hash);
  }
}
