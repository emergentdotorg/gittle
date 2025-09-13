package org.emergent.maven.gitver.core.version;

import java.util.Map;
import lombok.Value;
import org.emergent.maven.gitver.core.Constants;
import org.emergent.maven.gitver.core.Mapper;
import org.emergent.maven.gitver.core.Util;

import static org.emergent.maven.gitver.core.Constants.*;

@Value
@lombok.Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class RefData {

  String branch;
  String hash;
  BasicVersion version;

  public String hashShort() {
    return Util.toShortHash(hash);
  }

  public String getHashShort() {
    return hashShort();
  }

  public int major() {
    return version.getMajor();
  }

  public int minor() {
    return version.getMinor();
  }

  public int patch() {
    return version.getPatch();
  }

  public int commit() {
    return version.getCommit();
  }

  public String branch() {
    return getBranch();
  }

  public String hash() {
    return getHash();
  }

  public Map<String, String> toProperties() {
    Mapper m = Mapper.create()
      .put(Constants.GITVER_BRANCH, getBranch())
      .put(GITVER_HASH, getHash())
      .put(Constants.GITVER_HASH_SHORT, getHashShort())
      .put(GITVER_MAJOR, version.getMajor(), -1)
      .put(GITVER_MINOR, version.getMinor(), -1)
      .put(GITVER_PATCH, version.getPatch(), -1)
      .put(GITVER_COMMIT_NUMBER, version.getCommit(), -1);
;
    return m.toMap();
  }
}
