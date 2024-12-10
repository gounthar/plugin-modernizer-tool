package io.jenkins.tools.pluginmodernizer.core.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Paths;
import java.net.URL;
import java.net.MalformedURLException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openrewrite.Recipe;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class SettingsEnvTest {

    @SystemStub
    private EnvironmentVariables envVars = new EnvironmentVariables()
            .set("CACHE_DIR", ".my-cache")
            .remove("GH_TOKEN")
            .set("GITHUB_TOKEN", "fake-token")
            .set("GH_OWNER", "fake-org")
            .set("JENKINS_UC", "https://example-com")
            .remove("MAVEN_HOME")
            .set("M2_HOME", "/opt/maven-test");

    @Test
    public void testCustomCache() throws Exception {
        assertEquals(Paths.get(".my-cache", Settings.CACHE_SUBDIR), Settings.DEFAULT_CACHE_PATH);
    }

    @Test
    public void testCustomM2Home() throws Exception {
        assertEquals(Paths.get("/opt/maven-test"), Settings.DEFAULT_MAVEN_HOME);
    }

    @Test
    public void testGithubToken() throws Exception {
        assertEquals("fake-token", Settings.GITHUB_TOKEN);
    }

    @Test
    public void testUpdateCenter() throws Exception {
        assertEquals("https://example-com", Settings.DEFAULT_UPDATE_CENTER_URL.toString());
    }

    @Test
    public void testGithubOwner() throws Exception {
        assertEquals("fake-org", Settings.GITHUB_OWNER);
    }

    @Test
    public void ensureAllRecipesHaveAttributes() {
        for (Recipe recipe : Settings.AVAILABLE_RECIPES) {
            assertNotNull(recipe.getName(), "Recipe name is null");
            assertNotNull(recipe.getDisplayName(), "Recipe display name is null for " + recipe.getName());
            assertNotNull(recipe.getDescription(), "Recipe description is null for " + recipe.getName());
            assertNotNull(recipe.getTags(), "Recipe tags are null for " + recipe.getName());
            assertFalse(recipe.getTags().isEmpty(), "Recipe tags are empty for " + recipe.getName());
        }
    }

    @Test
    public void testPluginStatsInstallationsUrl() throws Exception {
        String expectedUrl = "https://stats.jenkins.io/jenkins-stats/svg/202410-plugins.csv";
        URL actualUrl = Settings.DEFAULT_PLUGINS_STATS_INSTALLATIONS_URL;
        assertEquals(expectedUrl, actualUrl.toString());
    }

    @Test
    public void testPluginStatsInstallationsUrlWithRetryAndFallback() throws Exception {
        envVars.set("JENKINS_PLUGINS_STATS_INSTALLATIONS_URL", "invalid-url");
        URL url = null;
        try {
            url = Settings.DEFAULT_PLUGINS_STATS_INSTALLATIONS_URL;
        } catch (MalformedURLException e) {
            // Expected exception
        }
        assertNotNull(url);
        assertEquals("https://stats.jenkins.io/jenkins-stats/svg/202410-plugins.csv", url.toString());
    }
}
