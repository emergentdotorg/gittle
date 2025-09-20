package org.emergent.maven.gitver.core;

import java.beans.XMLDecoder;
import java.beans.XMLEncoder;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;

@UtilityClass
public class XmlCodec {

    public static <T> String write(T bean) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                XMLEncoder encoder = new XMLEncoder(new BufferedOutputStream(baos))) {
            encoder.writeObject(bean);
            encoder.close();
            return baos.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new GitverException(e.getMessage(), e);
        }
    }

    public static <T> T read(String s) {
        try (InputStream is = new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
                XMLDecoder decoder = new XMLDecoder(is)) {
            return readObject(decoder);
        } catch (IOException e) {
            throw new GitverException(e.getMessage(), e);
        }
    }

    private static <T> T readObject(XMLDecoder decoder) {
        //noinspection unchecked
        return (T) decoder.readObject();
    }
}
