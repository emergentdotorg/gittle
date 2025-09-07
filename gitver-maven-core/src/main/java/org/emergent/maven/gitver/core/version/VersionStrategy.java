package org.emergent.maven.gitver.core.version;

import org.emergent.maven.gitver.core.VersionConfig;

public interface VersionStrategy {

  VersionConfig getVersionConfig();

  RefVersionData getRefVersionData();

  String toVersionString();
}
