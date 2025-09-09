package org.emergent.maven.gitver.core.version;

import java.util.Map;

public interface VersionStrategy {

  String GITVER_VERSION_FULL = "gitver.version";

  String toVersionString();

  Map<String, String> toProperties();
}
