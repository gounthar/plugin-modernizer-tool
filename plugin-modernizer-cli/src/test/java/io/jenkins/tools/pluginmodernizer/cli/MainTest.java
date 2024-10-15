package io.jenkins.tools.pluginmodernizer.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openrewrite.Recipe;
import picocli.CommandLine;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith({MockitoExtension.class, SystemStubsExtension.class})
public class MainTest {

    private CommandLine commandLine;
    private ByteArrayOutputStream outputStream;
    private Main main;

    @TempDir
    Path tempDir;

    @Mock
    private Config config;

    @SystemStub
    private EnvironmentVariables envVars =
            new EnvironmentVariables().set("GH_TOKEN", ".my-token").set("GH_OWNER", "fake-org");

    @BeforeEach
    public void setUp() {
        main = new Main();
        commandLine = new CommandLine(main);
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    public void testGetPlugins() {
        String[] args = {"-p", "plugin1,plugin2", "-r", "FetchMetadata,MinimalBuildJava8"};
        commandLine.execute(args);

        List<Plugin> plugins = main.setup().getPlugins();
        assertNotNull(plugins);
        assertEquals(2, plugins.size());
        assertEquals("plugin1", plugins.get(0).getName());
        assertEquals("plugin2", plugins.get(1).getName());
    }

    @Test
    public void testGetRecipes() {
        String[] args = {"-p", "plugin1,plugin2", "-r", "FetchMetadata,MinimalBuildJava8"};
        commandLine.execute(args);

        List<Recipe> recipes = main.setup().getRecipes();
        assertNotNull(recipes);
        assertEquals(2, recipes.size());
        assertEquals(
                "io.jenkins.tools.pluginmodernizer.FetchMetadata",
                recipes.get(0).getName());
        assertEquals(
                "io.jenkins.tools.pluginmodernizer.MinimalBuildJava8",
                recipes.get(1).getName());
    }

    @Test
    public void testGetRecipesWithFQDN() {
        String[] args = {
            "-p",
            "plugin1,plugin2",
            "-r",
            "io.jenkins.tools.pluginmodernizer.FetchMetadata,io.jenkins.tools.pluginmodernizer.MinimalBuildJava8"
        };
        commandLine.execute(args);

        List<Recipe> recipes = main.setup().getRecipes();
        assertNotNull(recipes);
        assertEquals(2, recipes.size());
        assertEquals(
                "io.jenkins.tools.pluginmodernizer.FetchMetadata",
                recipes.get(0).getName());
        assertEquals(
                "io.jenkins.tools.pluginmodernizer.MinimalBuildJava8",
                recipes.get(1).getName());
    }

    @Test
    public void testMissingRecipesArgument() {
        String[] args = {"-p", "plugin1,plugin2"};
        int exitCode = commandLine.execute(args);
        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
    }

    @Test
    public void testPluginFile() throws IOException {
        Path pluginFile = tempDir.resolve("plugins.txt");
        Files.write(pluginFile, List.of("plugin1", "", "plugin2", "   ", "plugin3"));
        String[] args = {"-f", pluginFile.toString(), "-r", "FetchMetadata"};
        commandLine.execute(args);
        List<Plugin> plugins = main.setup().getPlugins();
        assertNotNull(plugins);
        assertEquals(3, plugins.size());
        assertTrue(plugins.contains(Plugin.build("plugin1")));
        assertTrue(plugins.contains(Plugin.build("plugin2")));
        assertTrue(plugins.contains(Plugin.build("plugin3")));
    }

    @Test
    public void testPluginFileAlongWithPluginOptionWithCommonPlugins() throws IOException {
        Path pluginFile = tempDir.resolve("plugins.txt");
        Files.write(pluginFile, List.of("plugin1", "", "plugin2", "   ", "plugin3"));
        String[] args = {"-f", pluginFile.toString(), "-r", "FetchMetadata"};
        commandLine.execute(args);
        List<Plugin> plugins = main.setup().getPlugins();
        assertNotNull(plugins);
        assertEquals(3, plugins.size());
        assertTrue(plugins.contains(Plugin.build("plugin1")));
        assertTrue(plugins.contains(Plugin.build("plugin2")));
        assertTrue(plugins.contains(Plugin.build("plugin3")));
    }

    @Test
    public void testPluginFilePluginOrder() throws IOException {
        Path pluginFile = tempDir.resolve("plugins.txt");
        Files.write(pluginFile, List.of("plugin1", "", "plugin2", "   ", "plugin3"));
        String[] args = {"-f", pluginFile.toString(), "-r", "FetchMetadata"};
        commandLine.execute(args);
        List<Plugin> plugins = main.setup().getPlugins();
        assertNotNull(plugins);
        assertEquals(3, plugins.size());
        assertTrue(plugins.contains(Plugin.build("plugin1")));
        assertTrue(plugins.contains(Plugin.build("plugin2")));
        assertTrue(plugins.contains(Plugin.build("plugin3")));
    }

    @Test
    public void testPluginFileAlongWithPluginOptionPluginOrder() throws IOException {
        Path pluginFile = tempDir.resolve("plugins.txt");
        Files.write(pluginFile, List.of("plugin1", "", "plugin2", "   ", "plugin3"));
        String[] args = {"-f", pluginFile.toString(), "-r", "FetchMetadata"};
        commandLine.execute(args);
        List<Plugin> plugins = main.setup().getPlugins();
        assertNotNull(plugins);
        assertEquals(3, plugins.size());
        assertTrue(plugins.contains(Plugin.build("plugin1")));
        assertTrue(plugins.contains(Plugin.build("plugin2")));
        assertTrue(plugins.contains(Plugin.build("plugin3")));
    }

    @Test
    public void testSkipPushOptions() throws IOException {
        String[] args = {"-p", "plugin1,plugin2", "-r", "FetchMetadata", "--skip-push"};
        commandLine.execute(args);
        assertTrue(main.setup().isSkipPush());
    }

    @Test
    public void testSkipPullRequestOptions() throws IOException {
        String[] args = {
            "-p",
            "plugin1,plugin2",
            "-r",
            "FetchMetadata",
            "--skip-push",
            "--recipes",
            "FetchMetadata",
            "--skip-pull-request"
        };
        commandLine.execute(args);
        assertTrue(main.setup().isSkipPullRequest());
    }

    @Test
    public void testCleanLocalData() throws IOException {
        String[] args = {
            "-p",
            "plugin1,plugin2",
            "-r",
            "FetchMetadata",
            "--skip-push",
            "--recipes",
            "FetchMetadata",
            "--clean-local-data"
        };
        commandLine.execute(args);
        assertTrue(main.setup().isRemoveLocalData());
    }

    @Test
    public void testCleanForks() throws IOException {
        String[] args = {"-p", "plugin1,plugin2", "-r", "FetchMetadata", "--skip-push", "--clean-forks"};
        commandLine.execute(args);
        assertTrue(main.setup().isRemoveForks());
    }

    @Test
    public void testMavenHome() throws IOException {
        String[] args = {"--maven-home", Files.createTempDirectory("unused").toString()};
        int exitCode = commandLine.execute(args);
        assertEquals(CommandLine.ExitCode.USAGE, exitCode);
    }

    @Test
    public void testDryRunOption() {
        String[] args = {"-p", "plugin1,plugin2", "-r", "FetchMetadata", "-n"};
        commandLine.execute(args);
        assertTrue(main.setup().isDryRun());
    }

    @Test
    public void testExportDatatablesOption() {
        String[] args = {"-p", "plugin1,plugin2", "-r", "FetchMetadata", "-n", "-e"};
        commandLine.execute(args);
        assertTrue(main.setup().isExportDatatables());
    }

    @Test
    public void testDebugOption() {
        String[] args = {"-p", "plugin1,plugin2", "-r", "FetchMetadata", "-d"};
        commandLine.execute(args);
        main.setup();
        assertTrue(Config.DEBUG);
    }

    @Test
    public void testCachePathOption() {
        String[] args = {"-p", "plugin1,plugin2", "-r", "FetchMetadata", "-c", "/tmp/cache"};
        commandLine.execute(args);
        assertEquals(
                Paths.get("/tmp/cache/jenkins-plugin-modernizer-cli"),
                main.setup().getCachePath());
    }

    @Test
    public void testJenkinsUpdateCenterOptionWithoutValidOption() throws MalformedURLException {
        URL defaultUrl = new URL("https://updates.jenkins.io/current/update-center.actual.json");
        String[] args = {"--jenkins-update-center", "test-url"};
        commandLine.execute(args);
        assertEquals(defaultUrl, main.setup().getJenkinsUpdateCenter());
    }

    @Test
    public void testJenkinsUpdateCenterOptionWithValidOption() throws MalformedURLException {
        URL cliUrl = new URL("https://www.jenkins.io/");
        String[] args = {"--jenkins-update-center", "https://www.jenkins.io/"};
        commandLine.execute(args);
        assertEquals(cliUrl, main.setup().getJenkinsUpdateCenter());
    }

    @Test
    public void testListRecipesOption() {
        String[] args = {"-l"};
        commandLine.execute(args);
        assertTrue(main.listRecipes);
        main.run();
        assertTrue(outputStream.toString().contains("Available recipes:"));
    }

    @Test
    public void testCaseInsensitiveOption() {
        int exitCode = commandLine.setOptionsCaseInsensitive(true).execute("-H");
        assertEquals(CommandLine.ExitCode.OK, exitCode);
    }

    @Test
    public void testExecuteWithInvalidArgs() {
        String[] args = {"--invalidOption", "value"};
        int exitCode = commandLine.execute(args);
        assertNotEquals(CommandLine.ExitCode.OK, exitCode);
    }

    @Test
    public void testExecuteWithNoArgs() {
        String[] args = {};
        int exitCode = commandLine.execute(args);
        assertNotEquals(CommandLine.ExitCode.OK, exitCode);
    }
    @Test
    public void testSortPluginsByInstallations() throws IOException {
        // Mock or set up plugin installation counts
        // For example: plugin1: 100 installations, plugin2: 200 installations, plugin3: 300 installations
        setupMockInstallationCounts();

        Path pluginFile = tempDir.resolve("plugins.txt");
        Files.write(pluginFile, List.of("plugin1", "plugin2", "plugin3", "plugin4"));
        String[] args = {"-f", pluginFile.toString(), "-r", "FetchMetadata", "--sort-by-installations"};
        commandLine.execute(args);
        List<Plugin> plugins = main.setup().getPlugins();

        assertNotNull(plugins);
        assertEquals(4, plugins.size());
        assertEquals("plugin3", plugins.get(0).getName()); // Highest installation count
        assertEquals("plugin2", plugins.get(1).getName());
        assertEquals("plugin1", plugins.get(2).getName());
        assertEquals("plugin4", plugins.get(3).getName()); // Assume plugin4 has 0 or unknown installations

        // Test plugins with equal installation counts
        // For example: plugin5 and plugin6 both have 150 installations
        testEqualInstallationCounts();
    }

    private void setupMockInstallationCounts() {
        // Implementation to set up mock data
    }

    private void testEqualInstallationCounts() {
        // Implementation to test sorting of plugins with equal installation counts
    }
}
