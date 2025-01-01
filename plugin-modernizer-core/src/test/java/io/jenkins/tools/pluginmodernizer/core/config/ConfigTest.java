package io.jenkins.tools.pluginmodernizer.core.config;

import static org.junit.jupiter.api.Assertions.*;

import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.model.Recipe;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ConfigTest {

    @Test
    public void testConfigBuilderWithAllFields() throws MalformedURLException {
        String version = "1.0";
        String githubOwner = "test-owner";
        List<Plugin> plugins =
                Stream.of("plugin1", "plugin2").map(Plugin::build).toList();
        Recipe recipe = Mockito.mock(Recipe.class);
        Mockito.doReturn("recipe1").when(recipe).getName();
        URL jenkinsUpdateCenter = new URL("https://updates.jenkins.io/current/update-center.actual.json");
        Path cachePath = Paths.get("path/to/cache");
        Path mavenHome = Paths.get("path/to/maven");
        boolean dryRun = true;

        Config config = Config.builder()
                .withVersion(version)
                .withGitHubOwner(githubOwner)
                .withPlugins(plugins)
                .withRecipe(recipe)
                .withJenkinsUpdateCenter(jenkinsUpdateCenter)
                .withCachePath(cachePath)
                .withMavenHome(mavenHome)
                .withDryRun(dryRun)
                .withRemoveForks(true)
                .build();

        assertEquals(version, config.getVersion());
        assertEquals(githubOwner, config.getGithubOwner());
        assertEquals(plugins, config.getPlugins());
        assertEquals(recipe, config.getRecipe());
        assertEquals(jenkinsUpdateCenter, config.getJenkinsUpdateCenter());
        assertEquals(cachePath.toAbsolutePath(), config.getCachePath());
        assertEquals(mavenHome.toAbsolutePath(), config.getMavenHome());
        assertTrue(config.isRemoveForks());
        assertTrue(config.isRemoveForks());
        assertTrue(config.isDryRun());
        assertEquals("https://api.github.com", config.getGithubApiUrl().toString());
    }

    @Test
    public void testConfigBuilderWithDefaultValues() {
        Config config = Config.builder().build();

        assertNull(config.getVersion());
        assertNull(config.getPlugins());
        assertNull(config.getRecipe());
        assertEquals(Settings.DEFAULT_UPDATE_CENTER_URL, config.getJenkinsUpdateCenter());
        assertEquals(Settings.DEFAULT_CACHE_PATH, config.getCachePath());
        assertEquals(Settings.DEFAULT_MAVEN_HOME, config.getMavenHome());
        assertFalse(config.isRemoveForks());
        assertFalse(config.isRemoveForks());
        assertFalse(config.isDryRun());
    }

    @Test
    public void testConfigBuilderWithPartialValues() {
        String version = "2.0";
        List<Plugin> plugins =
                Stream.of("plugin1", "plugin2").map(Plugin::build).toList();

        Config config =
                Config.builder().withVersion(version).withPlugins(plugins).build();

        assertEquals(version, config.getVersion());
        assertEquals(plugins, config.getPlugins());
        assertNull(config.getRecipe());
        assertEquals(Settings.DEFAULT_UPDATE_CENTER_URL, config.getJenkinsUpdateCenter());
        assertEquals(Settings.DEFAULT_CACHE_PATH, config.getCachePath());
        assertEquals(Settings.DEFAULT_MAVEN_HOME, config.getMavenHome());
        assertFalse(config.isDryRun());
    }

    @Test
    public void testConfigBuilderWithNullValues() {
        Config config = Config.builder()
                .withJenkinsUpdateCenter(null)
                .withCachePath(null)
                .withMavenHome(null)
                .build();

        assertNull(config.getVersion());
        assertNull(config.getPlugins());
        assertNull(config.getRecipe());
        assertEquals(Settings.DEFAULT_UPDATE_CENTER_URL, config.getJenkinsUpdateCenter());
        assertEquals(Settings.DEFAULT_CACHE_PATH, config.getCachePath());
        assertEquals(Settings.DEFAULT_MAVEN_HOME, config.getMavenHome());
        assertFalse(config.isDryRun());
    }

    @Test
    public void testConfigBuilderDryRun() {
        Config config = Config.builder().withDryRun(true).build();

        assertTrue(config.isDryRun());
    }
}
