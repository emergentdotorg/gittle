package org.emergent.maven.gitver.extension;

import org.apache.maven.building.Source;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelProcessor;
import org.apache.maven.model.building.ModelProcessor;
import org.eclipse.sisu.Priority;
import org.eclipse.sisu.Typed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Map;
import javax.inject.Named;
import javax.inject.Singleton;

@Priority(1)
@Named("core-default")
@Singleton
@Typed(ModelProcessor.class)
public class GitverModelProcessor extends DefaultModelProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(GitverModelProcessor.class);

  private final ModelProcessingContext processingContext = new ModelProcessingContext();

  @Override
  public Model read(File input, Map<String, ?> options) throws IOException {
    return processModel(super.read(input, options), options);
  }

  @Override
  public Model read(Reader input, Map<String, ?> options) throws IOException {
    return processModel(super.read(input, options), options);
  }

  @Override
  public Model read(InputStream input, Map<String, ?> options) throws IOException {
    return processModel(super.read(input, options), options);
  }

  private Model processModel(Model projectModel, Map<String, ?> options) {
    if (Util.isDisabled()) {
      LOGGER.info("GitverModelProcessor.processModel: disabled");
      return projectModel;
    }

    File projDir = projectModel.getProjectDirectory();
    if (projDir == null) {
      File projPom = projectModel.getPomFile();
      if (projPom != null) {
        projDir = projPom.getAbsoluteFile().getParentFile();
      } else {
        Source pomSource = (Source)options.get(ModelProcessor.SOURCE);
        if (pomSource != null) {
          projPom = new File(pomSource.getLocation());
          projDir = projPom.getAbsoluteFile().getParentFile();
        }
      }
    }
    if (projDir != null) {
      Util.writePom(projectModel, projDir.toPath().resolve("original.pom.xml"));
    }

    return processingContext.processModel(projectModel, options);
  }
}
