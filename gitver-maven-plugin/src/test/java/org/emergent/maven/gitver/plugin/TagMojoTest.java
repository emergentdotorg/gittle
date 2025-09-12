package org.emergent.maven.gitver.plugin;

import java.io.File;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TagMojoTest extends AbstractMojoTest {

  @Test
  public void readDefaultParameters() throws Exception {
    File pom = new File("target/test-classes/project-to-test/");
    assertThat(pom).as("POM file").isNotNull().exists();
    TagMojo tag = (TagMojo)rule.lookupConfiguredMojo(pom, "tag");
    assertThat(tag).isNotNull();
    assertThat(tag.getNamePattern()).isEqualTo("v%v");
    assertThat(tag.getMessagePattern()).isEqualTo("Release version %v");
    assertThat(tag.isFailWhenExists()).isTrue();
  }

  @Test
  public void readPropertiesParameters() throws Exception {
    File pom = new File("target/test-classes/project-to-tag/");
    assertThat(pom).as("POM file").isNotNull().exists();
    TagMojo tag = (TagMojo)rule.lookupConfiguredMojo(pom, "tag");
    assertThat(tag).isNotNull();
    assertThat(tag.getNamePattern()).isEqualTo("version-%v");
    assertThat(tag.getMessagePattern()).isEqualTo("Release message %v");
    assertThat(tag.isFailWhenExists()).isFalse();
  }
}
