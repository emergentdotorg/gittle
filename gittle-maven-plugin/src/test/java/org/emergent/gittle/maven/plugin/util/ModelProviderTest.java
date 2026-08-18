package org.emergent.gittle.maven.plugin.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.apache.maven.model.Model;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ModelProviderTest {

    private Map<MavenProject, Model> models;

    private ModelProvider provider;

    @BeforeEach
    public void setUp() {
        models = new HashMap<>();
        provider = new ModelProvider(models);
    }

    @Test
    public void getModel_AlreadyExists() throws Exception {
        final MavenProject mavenProject = new MavenProject();
        final Model existing = new Model();
        models.put(mavenProject, existing);
        final Model model = provider.getModel(mavenProject);
        assertThat(model).isEqualTo(existing);
    }
}
