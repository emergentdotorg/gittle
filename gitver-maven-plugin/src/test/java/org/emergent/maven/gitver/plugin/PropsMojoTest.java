package org.emergent.maven.gitver.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.project.MavenProject;
import org.emergent.maven.gitver.core.Coordinates;
import org.junit.jupiter.api.Test;

public class PropsMojoTest extends AbstractMojoTest {

    public static class TestLog extends SilentLog {
        List<String> messages = new ArrayList<>();

        @Override
        public void warn(String message) {
            super.warn(message);
            messages.add(message);
        }

        @Override
        public boolean isInfoEnabled() {
            return true;
        }

        @Override
        public void info(CharSequence content) {
            super.info(content);
            messages.add(content.toString());
        }

        public List<String> getMessages() {
            return messages;
        }
    }

    @Test
    public void testPrint() throws Exception {
        File pom = new File("target/test-classes/project-to-test/");
        assertThat(pom).as("POM file").isNotNull().exists();

        PropsMojo propsMojo = (PropsMojo) rule.lookupConfiguredMojo(pom, "props");
        TestLog testLog = new TestLog();
        propsMojo.setLog(testLog);
        assertThat(propsMojo).isNotNull();
        propsMojo.execute();
        MavenProject proj = propsMojo.getMavenProject();
        String gitverVersion = proj.getProperties().getProperty("gitver.version");
        Coordinates gav = Coordinates.builder()
          .setGroupId(proj.getGroupId())
          .setArtifactId(proj.getArtifactId())
          .setVersion(proj.getVersion())
          .build();
        assertThat(testLog.getMessages()).isNotEmpty()
          .allMatch(s -> s.startsWith("Adding properties to project " + gav))
          .allMatch(s -> s.contains("gitver.version=" + gitverVersion + "\n"));
    }
}
