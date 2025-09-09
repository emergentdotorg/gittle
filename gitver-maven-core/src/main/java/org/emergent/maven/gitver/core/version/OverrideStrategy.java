package org.emergent.maven.gitver.core.version;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.emergent.maven.gitver.core.Util;
import org.emergent.maven.gitver.core.GitVerConfig;

public class OverrideStrategy implements VersionStrategy {
  private final String versionOverride;

  public static Optional<OverrideStrategy> from(GitVerConfig versionConfig) {
    return Optional.of(versionConfig).map(GitVerConfig::getVersionOverride)
      .filter(s -> !s.isBlank()).map(OverrideStrategy::new);
  }

  public OverrideStrategy(String versionOverride) {
    this.versionOverride = versionOverride;
  }

  @Override
  public String toVersionString() {
    return versionOverride;
  }

  @Override
  public Map<String, String> toProperties() {
    Map<String, Object> properties = new TreeMap<>();
    properties.put(GITVER_VERSION_FULL, toVersionString());
    properties.put(GitVerConfig.GV_VERSION_OVERRIDE, versionOverride);
    return Util.flatten(properties);
  }
}
