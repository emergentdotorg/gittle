package org.emergent.maven.gitver.core.version;

import java.util.Map;
import java.util.TreeMap;
import lombok.Getter;
import org.emergent.maven.gitver.core.Constants;


@Getter
public class OverrideStrategy implements VersionStrategy {

  private final String version;

  public OverrideStrategy(String version) {
    this.version = version;
  }

  @Override
  public String toVersionString() {
    return version;
  }

  @Override
  public Map<String, String> toProperties() {
    TreeMap<String, String> map = new TreeMap<>();
    map.put(Constants.GITVER_VERSION, toVersionString());
    return map;
  }
}
