package org.emergent.maven.gitver.extension;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.emergent.maven.gitver.core.GitverException;

class ExtensionUtil {
    public static final String REVISION = "revision";
    public static final String $_REVISION = "${revision}";

    static Model readModelFromPom(Path pomPath) {
        try (InputStream inputStream = Files.newInputStream(pomPath)) {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            return reader.read(inputStream);
        } catch (IOException | XmlPullParserException e) {
            throw new GitverException(e.getMessage(), e);
        }
    }

    static void writeModelToPom(Model projectModel, Path newPomPath) {
        try (Writer fileWriter = Files.newBufferedWriter(newPomPath, Charset.defaultCharset())) {
            MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(fileWriter, projectModel);
        } catch (IOException e) {
            throw new GitverException(e.getMessage(), e);
        }
    }
}
