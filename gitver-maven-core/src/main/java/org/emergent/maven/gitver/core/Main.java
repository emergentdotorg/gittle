package org.emergent.maven.gitver.core;

import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;
import org.emergent.maven.gitver.core.git.GitUtil;
import org.emergent.maven.gitver.core.version.VersionStrategy;

@Slf4j
public class Main {

  public static void main(String[] args) {
    VersionStrategy strategy = GitUtil.getVersionStrategy(Paths.get("."), GitVerConfig.builder().build());
    System.out.printf("version: %s%n", strategy.toVersionString());
  }
}
