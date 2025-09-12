package org.emergent.maven.gitver.core.version;

import java.util.Map;
import java.util.TreeMap;
import lombok.Builder;
import org.emergent.maven.gitver.core.Constants;
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
    properties.put(Constants.GITVER_BRANCH, branch());
    properties.put(Constants.GITVER_HASH, hash());
    properties.put(Constants.GITVER_HASH_SHORT, getHashShort());
    properties.put(Constants.GITVER_MAJOR, major());
    properties.put(Constants.GITVER_MINOR, minor());
    properties.put(Constants.GITVER_PATCH, patch());
    properties.put(Constants.GITVER_COMMIT_NUMBER, commit());
    return Util.flatten(properties);
  }

}
