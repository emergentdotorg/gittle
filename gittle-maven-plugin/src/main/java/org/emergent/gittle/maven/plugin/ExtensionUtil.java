package org.emergent.gittle.maven.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Strings;
import org.apache.maven.model.Build;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

@Slf4j
public class ExtensionUtil {
  public static final String REVISION = "revision";
  public static final String $_REVISION = "${revision}";

  static Model readModelFromPom(Path pomPath) {
    try (InputStream inputStream = Files.newInputStream(pomPath)) {
      MavenXpp3Reader reader = new MavenXpp3Reader();
      return reader.read(inputStream);
    } catch (IOException | XmlPullParserException e) {
      throw new GittleException(e.getMessage(), e);
    }
  }

  static void writeModelToPom(Model projectModel, Path newPomPath) {
    try (Writer fileWriter = Files.newBufferedWriter(newPomPath, Charset.defaultCharset())) {
      MavenXpp3Writer writer = new MavenXpp3Writer();
      writer.write(fileWriter, projectModel);
    } catch (IOException e) {
      throw new GittleException(e.getMessage(), e);
    }
  }

  // public static Xpp3Dom toXml(Config src) {
  //   JsonElement json = PropCodec.toJsonTree(src, Config.class);
  //   Map<String, Object> map = PropCodec.fromJsonTree(json, OBJ_MAP_TT.getType());
  //   return toXml("configuration", map);
  // }
  //
  // private static Xpp3Dom toXml(String name, Object value) {
  //   Xpp3Dom dom = new Xpp3Dom(name);
  //   if (value instanceof Map<?, ?> m) {
  //     toXml(m).forEach(dom::addChild);
  //   } else if (value != null) {
  //     dom.setValue(String.valueOf(value));
  //   }
  //   return dom;
  // }
  //
  // private static List<Xpp3Dom> toXml(Map<?, ?> map) {
  //   return map.entrySet().stream()
  //       .filter(e -> e.getKey() instanceof String)
  //       .filter(e -> Objects.nonNull(e.getValue()))
  //       .map(e -> toXml((String) e.getKey(), e.getValue()))
  //       .toList();
  // }

  public static boolean copyVersions(Model src, Model tgt) {
    String versionString = Optional.ofNullable(src.getProperties().getProperty(REVISION))
        .filter(v -> !$_REVISION.equals(v)).orElse(null);
    if (versionString == null && Optional.ofNullable(tgt.getVersion())
        .filter($_REVISION::equals).isPresent()) {
      versionString = src.getVersion();
    }
    if (versionString == null && Optional.ofNullable(tgt.getParent()).map(Parent::getVersion)
        .filter($_REVISION::equals).isPresent()) {
      versionString = Optional.ofNullable(src.getParent()).map(Parent::getVersion).orElse(null);
    }
    return versionString != null && replaceRevision(tgt, versionString);
  }

  public static boolean replaceRevision(Model model, String versionString) {
    Build build = Optional.ofNullable(model.getBuild()).orElseGet(() -> {
      model.setBuild(new Build());
      return model.getBuild();
    });
    PluginManagement pluginMgmt = Optional.ofNullable(build.getPluginManagement()).orElseGet(() -> {
      build.setPluginManagement(new PluginManagement());
      return build.getPluginManagement();
    });
    DependencyManagement depsMgmt = Optional.ofNullable(model.getDependencyManagement()).orElseGet(() -> {
      model.setDependencyManagement(new DependencyManagement());
      return model.getDependencyManagement();
    });
    Properties properties = model.getProperties();

    AtomicReference<Boolean> modified = new AtomicReference<>(false);

    Supplier<String> ver = () -> {
      modified.set(true);
      return versionString;
    };

    if (Optional.ofNullable(model.getVersion()).filter($_REVISION::equals).isPresent()) {
      model.setVersion(ver.get());
    }

    if (Optional.ofNullable(model.getParent()).map(Parent::getVersion).filter($_REVISION::equals).isPresent()) {
      Parent parent = model.getParent();
      log.info("Setting parent {} version to {}", parent, versionString);
      model.getParent().setVersion(ver.get());
    }

    properties.stringPropertyNames().stream()
        .map(k -> Map.entry(k, properties.getProperty(k)))
        .filter(e -> Util.contains(e.getValue(), $_REVISION))
        .forEach(e -> {
          String replacement = Strings.CS.replace(e.getValue(), $_REVISION, ver.get());
          properties.setProperty(e.getKey(), replacement);
        });

    Stream.concat(build.getPlugins().stream(), pluginMgmt.getPlugins().stream())
        .filter(p -> $_REVISION.equals(p.getVersion()))
        .forEach(p -> p.setVersion(ver.get()));

    Stream.concat(
            Stream.concat(model.getDependencies().stream(), depsMgmt.getDependencies().stream()),
            Stream.concat(build.getPlugins().stream(), pluginMgmt.getPlugins().stream())
                .flatMap(plugin -> plugin.getDependencies().stream())
        )
        .filter(d -> $_REVISION.equals(d.getVersion()))
        .forEach(d -> d.setVersion(ver.get()));

    if (properties.containsKey(REVISION)) {
      properties.setProperty(REVISION, ver.get());
    }

    return modified.get();
  }

}
