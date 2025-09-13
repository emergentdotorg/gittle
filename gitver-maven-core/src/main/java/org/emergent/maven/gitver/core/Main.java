package org.emergent.maven.gitver.core;

import java.nio.file.Path;
import java.nio.file.Paths;
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
    Path basedir = Paths.get(path);
    GitverConfig config = GitverConfig.from(new Properties());
    VersionStrategy strategy = StrategyFactory.getInstance(basedir).getVersionStrategy(config);
    System.out.printf("version: %s%n", strategy.toVersionString());
  }
}
