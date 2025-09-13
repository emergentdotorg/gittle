package org.emergent.maven.gitver.core.version;

import lombok.Value;
import org.emergent.maven.gitver.core.Util;

@Value
@lombok.Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class RefData {

  String branch;
  String hash;

  public String hashShort() {
    return Util.toShortHash(hash);
  }

  public String getHashShort() {
    return hashShort();
  }
}
