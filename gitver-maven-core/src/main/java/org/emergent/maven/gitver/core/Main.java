package org.emergent.maven.gitver.core;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.emergent.maven.gitver.core.version.StrategyFactory;
import org.emergent.maven.gitver.core.version.VersionStrategy;

@Slf4j
public class Main {

    private static final String DEFAULT_PATH = "../testgitver";

    public static void main(String[] args) {
        Package pack = Util.class.getPackage();
        String implVendor = pack.getImplementationVendor();
        String implVersion = pack.getImplementationVersion();
        System.out.printf("Vendor: %s ; Version: %s%n", implVendor, implVersion);
        String path = args.length > 0 ? args[0] : DEFAULT_PATH;
        File basedir = Paths.get(path).toAbsolutePath().toFile();
        GitverConfig config = GitverConfig.from(Collections.emptyMap());
        VersionStrategy strategy = StrategyFactory.getVersionStrategy(basedir, config);
        System.out.printf("version: %s%n", strategy.toVersionString());
    }
}
