package org.emergent.maven.gitver.core.version;

import java.util.Map;
import java.util.Properties;
import org.emergent.maven.gitver.core.GitverConfig;

public interface VersionStrategy {

    Map<String, String> asMap();
    String toVersionString();

    GitverConfig getConfig();

    Properties toProperties();
}
