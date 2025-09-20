package org.emergent.maven.gitver.extension;

import static org.emergent.maven.gitver.core.Constants.GV_VERSION_OVERRIDE;
import static org.emergent.maven.gitver.core.Constants.RELEASE_BRANCHES_DEF;
import static org.emergent.maven.gitver.core.Constants.TAG_PATTERN_DEF;
import static org.emergent.maven.gitver.core.Constants.VERSION_PATTERN_DEF;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Guice;
import com.google.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import javax.inject.Named;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.eclipse.sisu.EagerSingleton;
import org.eclipse.sisu.space.SpaceModule;
import org.eclipse.sisu.space.URLClassSpace;
import org.eclipse.sisu.wire.WireModule;
import org.emergent.maven.gitver.core.Constants;
import org.emergent.maven.gitver.core.GitverException;
import org.emergent.maven.gitver.core.XmlCodec;

@Mojo(name = "yoyoma")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "set", toBuilder = true, builderClassName = "Builder")
public class GitverConfigBase implements org.apache.maven.plugin.Mojo {

    public static final Set<Class<?>> PRIMITIVE_TYPES = Set.of(Boolean.class, String.class, Number.class);
    public static final Set<Class<?>> ALLOWED_TYPES = new HashSet<>(Set.of(List.class, Map.class));

    static {
        ALLOWED_TYPES.addAll(PRIMITIVE_TYPES);
    }

    private Log log = null;

    @Inject(optional = true)
    StatusProvider statusProvider;

    @Parameter(defaultValue = TAG_PATTERN_DEF, property = Constants.GV_TAG_PATTERN)
    protected String tagPattern;

    @Parameter(defaultValue = GV_VERSION_OVERRIDE, property = GV_VERSION_OVERRIDE)
    String versionOverride;

    @Parameter(defaultValue = VERSION_PATTERN_DEF, property = Constants.GV_VERSION_PATTERN)
    String versionPattern;

    @Parameter(defaultValue = RELEASE_BRANCHES_DEF, property = Constants.GV_RELEASE_BRANCHES)
    String releaseBranches;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {}

    @Override
    public void setLog(Log log) {}

    @Override
    public Log getLog() {
        return null;
    }

    public Map<String, Object> toProperties() {
        return PropertiesCodec.toMap(this);
    }

    public static GitverConfigBase loadProperties(Map<String, Object> map) {
        return PropertiesCodec.fromMap(map);
    }

    public String toXml() {
        return XmlCodec.write(this);
    }

    public static GitverConfigBase fromXml(String xml) {
        return XmlCodec.read(xml);
    }

    public static Set<String> getReleaseBranchesSet(String releaseBranches) {
        return Arrays.stream(releaseBranches.split(","))
                .map(String::trim)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @EagerSingleton
    @Named("status-provider")
    public static class StatusProvider {

        @Parameter(defaultValue = "main", property = "gv.branchName")
        String branch;

        @Parameter(defaultValue = "", property = "gv.hash")
        String hash;

        @Parameter(defaultValue = "0.0.0", property = "gv.tag")
        String tag;

        @Parameter(defaultValue = "", property = "gv.commits")
        int commits;

        @Parameter(defaultValue = "false", property = "gv.dirty")
        boolean dirty;

        public void run() {}

        public static void main(String[] args) {
            ClassLoader classloader = StatusProvider.class.getClassLoader();
            Guice.createInjector(
                    new WireModule( // auto-wires unresolved dependencies
                            new SpaceModule( // scans and binds @Named components
                                    new URLClassSpace(classloader) // abstracts class/resource finding
                                    )));
        }
    }

    public static class PropertiesCodec {

        private static final TypeToken<Map<String, Object>> MAP_TYPE_TOKEN = new TypeToken<>() {};
        private static final TypeToken<GitverConfigBase> CONFIG_BASE_TYPE_TOKEN = new TypeToken<>() {};
        private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

        public static Map<String, Object> toMap(GitverConfigBase bean) {
            JsonElement json = gson.toJsonTree(bean, CONFIG_BASE_TYPE_TOKEN.getType());
            return gson.fromJson(json, MAP_TYPE_TOKEN.getType());
        }

        public static GitverConfigBase fromMap(Map<String, Object> map) {
            JsonElement json = gson.toJsonTree(map, MAP_TYPE_TOKEN.getType());
            return gson.fromJson(json, CONFIG_BASE_TYPE_TOKEN.getType());
        }

        private static String propsToString(Properties props) {
            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                props.store(baos, null);
                return baos.toString(StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new GitverException(e.getMessage(), e);
            }
        }

        private static Properties stringToProps(String serialized) {
            try (InputStream is = new ByteArrayInputStream(serialized.getBytes(StandardCharsets.UTF_8))) {
                Properties props = new Properties();
                props.load(is);
                return props;
            } catch (IOException e) {
                throw new GitverException(e.getMessage(), e);
            }
        }
        // private static Set<String> getPropertyNames() {
        //     return Arrays.stream(PropertyUtils.getPropertyDescriptors(GitverConfigBase.class))
        //       .filter(pd -> ALLOWED_TYPES.stream().anyMatch(c -> c.isAssignableFrom(pd.getPropertyType())))
        //       .filter(pd -> pd.getPropertyType().getDeclaredAnnotation(Parameter.class) != null)
        //       .map(PropertyDescriptor::getName).collect(Collectors.toCollection(LinkedHashSet::new));
        // }
    }
}
