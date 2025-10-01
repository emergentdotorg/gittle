package org.emergent.maven.gitver.core.version;

import org.emergent.maven.gitver.core.GitverConfig;

import java.util.Map;

public interface VersionStrategy {

    GitverConfig getConfig();

    String toVersionString();

    Map<String, String> asMap();
}
