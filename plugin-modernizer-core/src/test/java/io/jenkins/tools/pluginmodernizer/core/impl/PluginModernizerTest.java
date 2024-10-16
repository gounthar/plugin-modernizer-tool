package io.jenkins.tools.pluginmodernizer.core.impl;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.github.GHService;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class PluginModernizerTest {

    private PluginModernizer pluginModernizer;
    private Plugin plugin;
    private GHService ghService;
    private PluginService pluginService;
    private CacheManager cacheManager;

    @BeforeEach
    public void setUp() {
        pluginModernizer = new PluginModernizer();
        plugin = mock(Plugin.class);
        ghService = mock(GHService.class);
        pluginService = mock(PluginService.class);
        cacheManager = mock(CacheManager.class);

        Config config = mock(Config.class);
        when(config.getCachePath()).thenReturn(Path.of("target"));

        pluginModernizer.config = config;
        pluginModernizer.ghService = ghService;
        pluginModernizer.pluginService = pluginService;
        pluginModernizer.cacheManager = cacheManager;
    }

    @Test
    public void testCreateDependabotConfigIfNotExists() {
        when(plugin.hasFile(".github/dependabot.yml")).thenReturn(false);

        pluginModernizer.createDependabotConfig(plugin);

        verify(plugin, times(1)).hasFile(".github/dependabot.yml");
        // Add additional verification for the creation logic if needed
    }

    @Test
    public void testDoNotCreateDependabotConfigIfExists() {
        when(plugin.hasFile(".github/dependabot.yml")).thenReturn(true);

        pluginModernizer.createDependabotConfig(plugin);

        verify(plugin, times(1)).hasFile(".github/dependabot.yml");
        // Add additional verification to ensure creation logic is not called
    }
}
