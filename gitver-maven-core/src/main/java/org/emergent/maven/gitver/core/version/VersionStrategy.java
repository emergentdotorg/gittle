package org.emergent.maven.gitver.core.version;

import java.util.Properties;
import org.emergent.maven.gitver.core.GitverConfig;

public interface VersionStrategy<T extends VersionStrategy<T>> {

    String toVersionString();

    GitverConfig getConfig();

    Properties toProperties();
}
