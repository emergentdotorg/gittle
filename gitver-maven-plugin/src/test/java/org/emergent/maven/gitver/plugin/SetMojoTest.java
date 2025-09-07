package org.emergent.maven.gitver.plugin;

import java.io.File;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SetMojoTest extends AbstractMojoTest {

  @Disabled
  @Test
  public void testSetVersion() throws Exception {
    File pom = new File("target/test-classes/project-to-test/");
    assertThat(pom).as("POM file").isNotNull().exists();

    SetMojo set = (SetMojo)rule.lookupConfiguredMojo(pom, "set");
    assertThat(set).isNotNull();
    //MojoExecutionException exception =
    //    catchThrowableOfType(() -> set.execute(), MojoExecutionException.class);
    //assertThat(exception).isNotNull().hasMessage("Cannot find .gitver.pom.xml");
  }
}
