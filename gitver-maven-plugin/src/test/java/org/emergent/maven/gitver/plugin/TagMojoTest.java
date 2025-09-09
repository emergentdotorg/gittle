package org.emergent.maven.gitver.plugin;

import java.io.File;
import org.emergent.maven.gitver.core.version.OverrideStrategy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TagMojoTest extends AbstractMojoTest {

  @Test
  public void readDefaultParameters() throws Exception {
    File pom = new File("target/test-classes/project-to-test/");
    assertThat(pom).as("POM file").isNotNull().exists();
    TagMojo tag = (TagMojo)rule.lookupConfiguredMojo(pom, "tag");
    assertThat(tag).isNotNull();
    assertThat(tag.getTagNamePattern()).isEqualTo("v%v");
    assertThat(tag.getTagMessagePattern()).isEqualTo("Release version %v");
    assertThat(tag.isFailWhenTagExist()).isTrue();
    assertThat(tag.replaceTokens("v%v", new OverrideStrategy("1.2.3").toVersionString())).isEqualTo("v1.2.3");
  }

  @Test
  public void readPropertiesParameters() throws Exception {
    File pom = new File("target/test-classes/project-to-tag/");
    assertThat(pom).as("POM file").isNotNull().exists();
    TagMojo tag = (TagMojo)rule.lookupConfiguredMojo(pom, "tag");
    assertThat(tag).isNotNull();
    assertThat(tag.getTagNamePattern()).isEqualTo("version-%v");
    assertThat(tag.getTagMessagePattern()).isEqualTo("Release message %v");
    assertThat(tag.isFailWhenTagExist()).isFalse();
  }
}
