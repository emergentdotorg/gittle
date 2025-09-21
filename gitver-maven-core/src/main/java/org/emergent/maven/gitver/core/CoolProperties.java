package org.emergent.maven.gitver.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import lombok.NonNull;
import org.eclipse.jgit.annotations.Nullable;

public class CoolProperties extends AbstractMap<String, String> {

    private final Map<String, String> impl = new LinkedHashMap<>();

    @Override
    public String put(String key, String value) {
        return impl.put(key, value);
    }

    @Override
    @NonNull
    public Set<Entry<String, String>> entrySet() {
        return impl.entrySet();
    }

    public void load(InputStream is) throws IOException {
        Properties props = new Properties();
        props.load(is);
        impl.putAll(Util.toStringStringMap(props));
    }

    public void store(OutputStream os, @Nullable String comment) throws IOException {
        Properties props = new Properties();
        props.putAll(impl);
        props.store(os, comment);
    }
}
