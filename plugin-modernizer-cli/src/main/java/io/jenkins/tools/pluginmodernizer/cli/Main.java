package io.jenkins.tools.pluginmodernizer.cli;

import com.google.inject.Guice;
import io.jenkins.tools.pluginmodernizer.core.GuiceModule;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.config.Settings;
import io.jenkins.tools.pluginmodernizer.core.impl.PluginModernizer;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.utils.PluginService;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openrewrite.Recipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "Plugin Modernizer",
        separator = " ",
        helpCommand = true,
        mixinStandardHelpOptions = true,
        versionProvider = PomVersionProvider.class,
        description = "Applies recipes to the plugins.",
        requiredOptionMarker = '*')
public class Main implements Runnable {

    static {
        System.setProperty("slf4j.internal.verbosity", "WARN");
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    private static Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        // Don't show the shutdown message for some args
        if (!Arrays.asList(args).contains("--version")
                && !Arrays.asList(args).contains("-v")
                && !Arrays.asList(args).contains("--list-recipes")
                && !Arrays.asList(args).contains("-l")
                && !Arrays.asList(args).contains("--help")
                && !Arrays.asList(args).contains("-h")) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> LOG.info("Plugin Modernizer finished.")));
        }
        new CommandLine(new Main()).setOptionsCaseInsensitive(true).execute(args);
    }

    /**
     * Plugin option that are mutually exclusive.
     */
    static class PluginOptions {
        @Option(
                names = {"-p", "--plugins"},
                description = "List of Plugins to Modernize.",
                split = ",",
                converter = PluginConverter.class)
        private List<Plugin> plugins;

        @Option(
                names = {"-f", "--plugin-file"},
                description = "Path to the file that contains a list of plugins.")
        private Path pluginFile;
    }

    @CommandLine.ArgGroup(exclusive = true, multiplicity = "1")
    private PluginOptions pluginOptions;

    @Option(
            names = {"-r", "--recipe"},
            required = true,
            description = "Recipe to be applied.",
            converter = RecipeConverter.class)
    private Recipe recipe;

    @Option(
            names = {"-g", "--github-owner"},
            description = "GitHub owner for forked repositories.")
    private String githubOwner = Settings.GITHUB_OWNER;

    @Option(
            names = {"--github-app-id"},
            description =
                    "GitHub App ID. If set you will need to set GH_APP_CLIENT_ID, GH_APP_CLIENT_SECRET, GH_APP_PRIVATE_KEY_FILE as environment variables to use JWT authentication. The app installation must be done on the given github owner (personal or organization).")
    public Long githubAppId;

    @Option(
            names = {"--github-app-source-installation-id"},
            description =
                    "GitHub App Installation ID for the source repositories. If set, the app installation must be done on the given github owner (personal or organization).")
    public Long githubAppSourceInstallationId;

    @Option(
            names = {"--github-app-target-installation-id"},
            description =
                    "GitHub App Installation ID for the target repositories. If set, the app installation must be done on the given github owner (personal or organization).")
    public Long githubAppTargetInstallationId;

    @Option(
            names = {"-n", "--dry-run"},
            description = "Perform a dry run without making any changes.")
    public boolean dryRun;

    @Option(
            names = {"--draft"},
            description = "Open a draft pull request.")
    public boolean draft;

    @Option(
            names = {"--skip-push"},
            description = "Skip pushing changes to the forked repositories. Always true if --dry-run is set.")
    public boolean skipPush;

    @Option(
            names = {"--skip-build"},
            description = "Skip building the plugins before and after modernization.")
    public boolean skipBuild;

    @Option(
            names = {"--skip-pull-request"},
            description = "Skip creating pull requests but pull changes to the fork. Always true if --dry-run is set.")
    public boolean skipPullRequest;

    @Option(
            names = {"--clean-local-data"},
            description = "Remove local plugin data before and after the modernization process.")
    public boolean removeLocalData;

    @Option(
            names = {"--clean-forks"},
            description =
                    "Remove forked repositories before and after the modernization process. Might cause data loss if you have other changes pushed on those forks. Forks with open pull request targeting original repo are not removed to prevent closing unmerged pull requests.")
    public boolean removeForks;

    @Option(
            names = {"-e", "--export-datatables"},
            description = "Creates a report or summary of the changes made through OpenRewrite.")
    public boolean exportDatatables;

    @Option(
            names = {"-d", "--debug"},
            description = "Enable debug logging.")
    public boolean debug;

    @Option(
            names = "--jenkins-update-center",
            description =
                    "Sets main update center; will override JENKINS_UC environment variable. If not set via CLI option or environment variable, will use default update center url.")
    public URL jenkinsUpdateCenter = Settings.DEFAULT_UPDATE_CENTER_URL;

    @Option(
            names = "--jenkins-plugin-info",
            description =
                    "Sets jenkins plugin version; will override JENKINS_PLUGIN_INFO environment variable. If not set via CLI option or environment variable, will use default plugin info")
    public URL jenkinsPluginVersions = Settings.DEFAULT_PLUGIN_VERSIONS;

    @Option(
            names = "--plugin-health-score",
            description =
                    "Sets the plugin health score URL; will override JENKINS_PHS environment variable. If not set via CLI option or environment variable, will use default health score url.")
    public URL pluginHealthScore = Settings.DEFAULT_HEALTH_SCORE_URL;

    @Option(
            names = "--jenkins-plugins-stats-installations-url",
            description =
                    "Sets the Jenkins stats top plugins URL; will override JENKINS_PLUGINS_STATS_INSTALLATIONS_URL environment variable. If not set via CLI option or environment variable, will use default Jenkins stats top plugins url.")
    public URL jenkinsPluginsStatsInstallationsUrl = Settings.DEFAULT_PLUGINS_STATS_INSTALLATIONS_URL;

    @Option(
            names = {"-c", "--cache-path"},
            description = "Path to the cache directory.")
    public Path cachePath = Settings.DEFAULT_CACHE_PATH;

    @Option(
            names = {"-m", "--maven-home"},
            description = "Path to the Maven Home directory.")
    public Path mavenHome = Settings.DEFAULT_MAVEN_HOME;

    @Option(
            names = {"-l", "--list-recipes"},
            help = true,
            description = "List available recipes.")
    public boolean listRecipes;

    public Config setup() {
        Config.DEBUG = debug;
        return Config.builder()
                .withVersion(getVersion())
                .withGitHubOwner(githubOwner)
                .withGitHubAppId(githubAppId)
                .withGitHubAppSourceInstallationId(githubAppSourceInstallationId)
                .withGitHubAppTargetInstallationId(githubAppTargetInstallationId)
                .withPlugins(pluginOptions != null ? pluginOptions.plugins : new ArrayList<>())
                .withRecipe(recipe)
                .withDryRun(dryRun)
                .withSkipPush(skipPush)
                .withSkipBuild(skipBuild)
                .withSkipPullRequest(skipPullRequest)
                .withDraft(draft)
                .withRemoveLocalData(removeLocalData)
                .withRemoveForks(removeForks)
                .withExportDatatables(exportDatatables)
                .withJenkinsUpdateCenter(jenkinsUpdateCenter)
                .withJenkinsPluginVersions(jenkinsPluginVersions)
                .withPluginHealthScore(pluginHealthScore)
                .withPluginStatsInstallations(jenkinsPluginsStatsInstallationsUrl)
                .withCachePath(
                        !cachePath.endsWith(Settings.CACHE_SUBDIR)
                                ? cachePath.resolve(Settings.CACHE_SUBDIR)
                                : cachePath)
                .withMavenHome(mavenHome)
                .build();
    }

    public String getVersion() {
        try {
            return new PomVersionProvider().getVersion()[0];
        } catch (Exception e) {
            LOG.error("Error getting version from pom.properties", e);
            return "unknown";
        }
    }

    public void listAvailableRecipes() {
        // Strip the FQDN prefix from the recipe name
        Settings.AVAILABLE_RECIPES.forEach(recipe -> LOG.info(
                "{} - {}",
                recipe.getName().replaceAll(Settings.RECIPE_FQDN_PREFIX + ".", ""),
                recipe.getDescription()));
    }

    private List<Plugin> loadPlugins() {
        if (pluginOptions == null) {
            return new ArrayList<>();
        }
        List<Plugin> loadedPlugins = new ArrayList<>();
        PluginService service = Guice.createInjector(new GuiceModule(setup())).getInstance(PluginService.class);
        if (pluginOptions.pluginFile != null) {
            List<Plugin> pluginsFromFile = service.loadPluginsFromFile(pluginOptions.pluginFile);
            if (pluginsFromFile != null) {
                loadedPlugins.addAll(pluginsFromFile);
            }
        }
        if (pluginOptions.plugins != null) {
            loadedPlugins.addAll(pluginOptions.plugins);
        }
        return loadedPlugins;
    }

    @Override
    public void run() {
        if (listRecipes) {
            listAvailableRecipes();
            return;
        }
        if (pluginOptions != null) {
            pluginOptions.plugins = loadPlugins();
        }
        LOG.info("Starting Plugin Modernizer");
        PluginModernizer modernizer =
                Guice.createInjector(new GuiceModule(setup())).getInstance(PluginModernizer.class);
        modernizer.start();
    }

    /**
     * Custom converter for Plugin class.
     */
    private static final class PluginConverter implements CommandLine.ITypeConverter<Plugin> {
        @Override
        public Plugin convert(String value) {
            if (value.trim().isBlank()) {
                return null;
            }
            return Plugin.build(value);
        }
    }

    /**
     * Custom converter for Recipe interface.
     */
    private static final class RecipeConverter implements CommandLine.ITypeConverter<Recipe> {
        @Override
        public Recipe convert(String value) {
            return Settings.AVAILABLE_RECIPES.stream()
                    // Compare without and without the FQDN prefix
                    .filter(recipe -> recipe.getName().equals(value)
                            || recipe.getName()
                                    .replace(Settings.RECIPE_FQDN_PREFIX + ".", "")
                                    .equals(value))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Invalid recipe name: " + value));
        }
    }
}
