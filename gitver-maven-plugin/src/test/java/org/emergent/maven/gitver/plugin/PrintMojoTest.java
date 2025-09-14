package org.emergent.maven.gitver.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.testing.SilentLog;
import org.junit.jupiter.api.Test;

public class PrintMojoTest extends AbstractMojoTest {

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

        PrintMojo printVersionMojo = (PrintMojo) rule.lookupConfiguredMojo(pom, "print");
        TestLog testLog = new TestLog();
        printVersionMojo.setLog(testLog);
        assertThat(printVersionMojo).isNotNull();
        printVersionMojo.execute();
        assertThat(testLog.getMessages()).isNotEmpty().allMatch(s -> s.startsWith("Gitver properties:"));
    }
}
