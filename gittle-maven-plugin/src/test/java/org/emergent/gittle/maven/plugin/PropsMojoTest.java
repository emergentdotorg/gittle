package org.emergent.gittle.maven.plugin;

import static org.apache.maven.shared.utils.logging.MessageUtils.buffer;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;

@MojoTest
public class PropsMojoTest {

    public static class TestLog extends SilentLog {
        List<String> messages = new ArrayList<>();

        @Override
        public void warn(String message) {
            super.warn(message);
            messages.add(message);
        }

        @Override
        public void warn(CharSequence content) {
            super.warn(content);
            messages.add(content.toString());
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

        @Override
        public void info(String message) {
            super.info(message);
            messages.add(message);
        }

        public List<String> getMessages() {
            return messages;
        }
    }

    @Inject
    private MavenSession session;

    @Test
    @InjectMojo(goal = "props", pom = "src/test/resources/project-to-test/pom.xml")
    public void testPrint(PropsMojo mojo) throws Exception {
        // File pom = new File("target/test-classes/project-to-test/");
        File pom = mojo.getMavenProject().getFile();
        assertThat(pom).as("POM file").isNotNull().exists();

        // PropsMojo propsMojo = (PropsMojo) rule.lookupConfiguredMojo(pom, PropsMojo.NAME);
        TestLog testLog = new TestLog();
        mojo.setLog(testLog);
        assertThat(mojo).isNotNull();
        mojo.execute();
        //String gav = "";
        //String gittleVersion = "";
        MavenProject proj = mojo.getMavenProject();
        // String normalVersion = proj.getProperties().getProperty(NORMAL_VERSION_PROPERTY);
        // assertThat(normalVersion).isNotBlank();
        Coordinates gav = Coordinates.builder()
                .setGroupId(proj.getGroupId())
                .setArtifactId(proj.getArtifactId())
                .setVersion(proj.getVersion())
                .build();
        testLog.warn(buffer()
               .a("--- ")
                       .mojo(gav)
                       .a(" ")
                       .strong("[build--extension]")
                       .a(" ---")
               .a(Util.join(proj.getProperties()))
               .a("--- ")
                       .strong("properties" )
               .a(" ---")
               .build());
        assertThat(testLog.getMessages()).isNotEmpty()
                .anyMatch(s -> s.startsWith("Adding properties to project " + gav))
                // .allMatch(s -> s.contains("gittle.resolved.version=" + gittleVersion + "\n"))
        ;
    }
}
