package org.emergent.maven.gitver.core.version;

import java.util.Map;
import java.util.TreeMap;
import lombok.Builder;
import org.emergent.maven.gitver.core.Util;

@Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public record RefVersionData(String branch, String hash, int major, int minor, int patch, int commit) {

  public String hashShort() {
    return Util.toShortHash(hash);
  }

  public String getHashShort() {
    return hashShort();
  }

  public Map<String, String> toProperties() {
    Map<String, Object> properties = new TreeMap<>();
    properties.put("gitver.branch", branch());
    properties.put("gitver.hash", hash());
    properties.put("gitver.hash.short", getHashShort());
    properties.put("gitver.major", major());
    properties.put("gitver.minor", minor());
    properties.put("gitver.patch", patch());
    properties.put("gitver.commitNumber", commit());
    return Util.flatten(properties);
  }

}
