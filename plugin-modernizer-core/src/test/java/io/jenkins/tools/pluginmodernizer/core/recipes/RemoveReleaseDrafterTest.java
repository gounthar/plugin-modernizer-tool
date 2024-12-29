package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.yaml.Assertions.yaml;

import io.jenkins.tools.pluginmodernizer.core.extractor.ArchetypeCommonFile;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link RemoveReleaseDrafter}.
 */
public class RemoveReleaseDrafterTest implements RewriteTest {

    @Test
    void shouldNotRemoveReleaseDrafter() {
        rewriteRun(
                spec -> spec.recipe(new RemoveReleaseDrafter()),
                // language=yaml
                yaml("{}", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.RELEASE_DRAFTER.getPath());
                }),
                yaml("{}", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.RELEASE_DRAFTER_WORKFLOW.getPath());
                }));
    }

    @Test
    void shouldRemoveReleaseDrafterIfContinuousDeliveryEnabled() {
        rewriteRun(
                spec -> spec.recipe(new RemoveReleaseDrafter()),
                // language=yaml
                yaml("{}", sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.WORKFLOW_CD.getPath());
                }),
                yaml("{}", null, sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.RELEASE_DRAFTER.getPath());
                }),
                yaml("{}", null, sourceSpecs -> {
                    sourceSpecs.path(ArchetypeCommonFile.RELEASE_DRAFTER_WORKFLOW.getPath());
                }));
    }
}
