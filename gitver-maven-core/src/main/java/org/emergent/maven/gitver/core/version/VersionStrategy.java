package org.emergent.maven.gitver.core.version;

import java.util.Map;
import org.emergent.maven.gitver.core.GitverConfig;

public interface VersionStrategy {

    GitverConfig config();

    String toVersionString();

    Map<String, String> getPropertiesMap();
}
