package org.emergent.maven.gitver.core.version;

import java.util.Map;

public interface VersionStrategy {

    String toVersionString();

    Map<String, String> getPropertiesMap();
}
