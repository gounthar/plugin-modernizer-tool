package io.jenkins.tools.pluginmodernizer.core.extractor;

import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.utils.UpdateCenterService;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flag for metadata
 */
public enum MetadataFlag {

    /**
     * If the SCM URL uses HTTPS
     */
    SCM_HTTPS(
            tag -> {
                if ("scm".equals(tag.getName())) {
                    Optional<String> connection = tag.getChildValue("connection");
                    return connection.isPresent() && connection.get().startsWith("scm:git:https");
                }
                return false;
            },
            null),

    /**
     * If the plugin uses HTTPS for all its repositories
     */
    MAVEN_REPOSITORIES_HTTPS(
            tag -> {
                if ("repositories".equals(tag.getName())) {
                    return tag.getChildren().stream()
                            .filter(c -> "repository".equals(c.getName()))
                            .map(Xml.Tag.class::cast)
                            .map(r -> r.getChildValue("url").orElseThrow())
                            .allMatch(url -> url.startsWith("https"));
                }
                return false;
            },
            null),

    /**
     * If the license block is set
     */
    LICENSE_SET(
            tag -> {
                if ("licenses".equals(tag.getName())) {
                    return tag.getChildren().stream()
                            .filter(c -> "license".equals(c.getName()))
                            .map(Xml.Tag.class::cast)
                            .map(r -> r.getChildValue("name").orElseThrow())
                            .findAny()
                            .isPresent();
                }
                return false;
            },
            null),

    /**
     * If the develop block is set
     */
    DEVELOPER_SET(
            tag -> {
                if ("developers".equals(tag.getName())) {
                    return tag.getChildren().stream()
                            .filter(c -> "developer".equals(c.getName()))
                            .map(Xml.Tag.class::cast)
                            .map(r -> r.getChildValue("id").orElseThrow())
                            .findAny()
                            .isPresent();
                }
                return false;
            },
            null),

    /**
     * If the plugin is an API plugin
     */
    IS_API_PLUGIN(null, (plugin, updateCenterService) -> updateCenterService.isApiPlugin(plugin)),

    /**
     * If the plugin is deprecated
     */
    IS_DEPRECATED(null, (plugin, updateCenterService) -> updateCenterService.isDeprecated(plugin)),
    ;

    /**
     * Function to check if the flag is applicable for the given XML tag
     */
    private final Predicate<Xml.Tag> isApplicableTag;

    /**
     * Function to check if the flag is applicable for the given plugin
     */
    private final BiPredicate<Plugin, UpdateCenterService> isApplicablePlugin;

    /**
     * Constructor
     * @param isApplicableTag Predicate to check if the flag is applicable for the given XML tag
     */
    MetadataFlag(Predicate<Xml.Tag> isApplicableTag, BiPredicate<Plugin, UpdateCenterService> isApplicablePlugin) {
        this.isApplicableTag = isApplicableTag;
        this.isApplicablePlugin = isApplicablePlugin;
    }

    /**
     * Check if the flag is applicable for the given XML tag
     * @param tag XML tag
     * @return true if the flag is applicable
     */
    public boolean isApplicable(Xml.Tag tag) {
        if (isApplicableTag == null) {
            return false;
        }
        return isApplicableTag.test(tag);
    }

    /**
     * Check if the flag is applicable for the given plugin
     * @param plugin Plugin
     * @return true if the flag is applicable
     */
    public boolean isApplicable(Plugin plugin, UpdateCenterService updateCenterService) {
        if (plugin.getMetadata() == null) {
            LOG.debug("Metadata not found for plugin {}", plugin.getName());
            return false;
        }
        if (plugin.getMetadata().hasFlag(this)) {
            LOG.debug("Flag {} already set for plugin {}", this, plugin.getName());
            return true;
        }
        if (isApplicablePlugin == null) {
            LOG.debug("No applicable plugin check for flag {}", this);
            return false;
        }
        boolean result = isApplicablePlugin.test(plugin, updateCenterService);
        LOG.debug("Flag {} applicable for plugin {}: {}", this, plugin.getName(), result);
        return result;
    }

    private static final Logger LOG = LoggerFactory.getLogger(MetadataFlag.class);
}
