package io.jenkins.tools.pluginmodernizer.core.impl;

import static org.mockito.Mockito.*;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class PluginModernizerTest {

    @Test
    public void testNoCompileSkipsCompilationAndVerification() {
        // Mock Recipe with requiresBuild = false
        Recipe recipe = mock(Recipe.class);
        when(recipe.requiresBuild()).thenReturn(false);
        when(recipe.getName()).thenReturn("test-recipe");

        // Mock Config with noCompile = true
        Config config = mock(Config.class);
        when(config.isNoCompile()).thenReturn(true);
        when(config.getRecipe()).thenReturn(recipe);
        when(config.getPlugins()).thenReturn(List.of(mock(Plugin.class)));
        when(config.isFetchMetadataOnly()).thenReturn(false);
        when(config.isSkipVerification()).thenReturn(false);

        // Spy on PluginModernizer to verify compilePlugin/verifyPlugin are not called
        PluginModernizer modernizer = Mockito.spy(new PluginModernizer());
        // Inject mocks
        modernizer.config = config;

        // Use a real plugin mock
        Plugin plugin = mock(Plugin.class);
        when(plugin.getName()).thenReturn("mock-plugin");
        when(plugin.getMetadata()).thenReturn(null);
        when(plugin.hasPreconditionErrors()).thenReturn(false);
        when(plugin.hasErrors()).thenReturn(false);
        when(plugin.hasMetadata()).thenReturn(false);

        // process should not call compilePlugin or verifyPlugin
        modernizer.process(plugin);

        verify(modernizer, never()).compilePlugin(any());
        verify(modernizer, never()).verifyPlugin(any());
    }
}
