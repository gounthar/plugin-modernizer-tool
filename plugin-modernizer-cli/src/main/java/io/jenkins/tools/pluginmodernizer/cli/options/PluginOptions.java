package io.jenkins.tools.pluginmodernizer.cli.options;

import io.jenkins.tools.pluginmodernizer.cli.converter.PluginConverter;
import io.jenkins.tools.pluginmodernizer.cli.converter.PluginFileConverter;
import io.jenkins.tools.pluginmodernizer.cli.converter.PluginPathConverter;
import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.model.ModernizerException;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Plugin option that are mutually exclusive.
 */
public final class PluginOptions implements IOption {

    /**
     * Logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PluginOptions.class);

    /**
     * List of plugins from CLI
     */
    @CommandLine.Option(
            names = {"--plugins"},
            description = "List of Plugins to Modernize.",
            split = ",",
            converter = PluginConverter.class)
    private List<Plugin> plugins;

    /**
     * List of plugins from file
     */
    @CommandLine.Option(
            names = {"--plugin-file"},
            description = "Path to the file that contains a list of plugins.",
            converter = PluginFileConverter.class)
    private List<Plugin> pluginsFromFile;

    /**
     * Path to a local plugin
     */
    @CommandLine.Option(
            names = {"--plugin-path"},
            description = "Path to the file that contains a list of plugins.",
            converter = PluginPathConverter.class)
    private Plugin pluginPath;

    @Override
    public void config(Config.Builder builder) {
        builder.withPlugins(getEffectivePlugins());
        if (pluginPath != null) {
            LOG.info("Running in dry-run because of local plugin: {}", pluginPath);
            builder.withDryRun(true);
        }
    }

    /**
     * Get effective plugins
     * @return List of plugins from CLI and/or file
     */
    private List<Plugin> getEffectivePlugins() {
        if (plugins == null) {
            plugins = List.of();
        }
        if (pluginsFromFile == null) {
            pluginsFromFile = List.of();
        }
        List<Plugin> effectivePlugins = Stream.concat(
                        pluginPath != null ? Stream.of(pluginPath) : Stream.empty(),
                        Stream.concat(plugins.stream(), pluginsFromFile.stream()))
                .toList();

        // Use current folder as plugin if no plugin is provided
        if (effectivePlugins.isEmpty()) {
            try {
                effectivePlugins.add(new PluginPathConverter().convert("."));
            } catch (Exception e) {
                throw new ModernizerException("Current directory doesn't seem to contains a plugin", e);
            }
        }
        return effectivePlugins;
    }
}
