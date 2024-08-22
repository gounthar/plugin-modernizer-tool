package io.jenkins.tools.pluginmodernizer.core.extractor;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.jenkins.tools.pluginmodernizer.core.model.JDK;
import io.jenkins.tools.pluginmodernizer.core.utils.JsonUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.openrewrite.ExecutionContext;
import org.openrewrite.FindSourceFiles;
import org.openrewrite.Preconditions;
import org.openrewrite.ScanningRecipe;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.TreeVisitor;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.marker.Markers;
import org.openrewrite.maven.MavenIsoVisitor;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.maven.tree.Parent;
import org.openrewrite.maven.tree.Pom;
import org.openrewrite.maven.tree.ResolvedPom;
import org.openrewrite.xml.tree.Xml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressFBWarnings(
        value = {"RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"},
        justification = "Extrac checks harmless")
public class MetadataCollector extends ScanningRecipe<MetadataCollector.MetadataAccumulator> {

    /**
     * LOGGER.
     */
    private static final Logger LOG = LoggerFactory.getLogger(MetadataCollector.class);

    @Override
    public String getDisplayName() {
        return "Plugin metadata extractor";
    }

    @Override
    public String getDescription() {
        return "Extracts metadata from plugin.";
    }

    /**
     * Accumulator to store metadata.
     */
    public static class MetadataAccumulator {
        private final List<ArchetypeCommonFile> commonFiles = new ArrayList<>();
        private final List<String> otherFiles = new ArrayList<>();
        private final List<MetadataFlag> flags = new LinkedList<>();
        private final List<JDK> jdkVersions = new ArrayList<>();

        public List<ArchetypeCommonFile> getCommonFiles() {
            return commonFiles;
        }

        public List<String> getOtherFiles() {
            return otherFiles;
        }

        public List<JDK> getJdkVersions() {
            return jdkVersions;
        }

        public void addCommonFile(ArchetypeCommonFile file) {
            commonFiles.add(file);
        }

        public void addOtherFile(String file) {
            otherFiles.add(file);
        }

        public void addJdk(JDK jdk) {
            jdkVersions.add(jdk);
        }

        public List<MetadataFlag> getFlags() {
            return flags;
        }

        public void addFlags(List<MetadataFlag> flags) {
            this.flags.addAll(flags);
        }
    }

    @Override
    public MetadataAccumulator getInitialValue(ExecutionContext ctx) {
        return new MetadataAccumulator();
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getScanner(MetadataAccumulator acc) {
        return new TreeVisitor<>() {
            @Override
            public Tree visit(Tree tree, ExecutionContext ctx) {
                if (tree == null) {
                    return null;
                }
                SourceFile sourceFile = (SourceFile) tree;
                ArchetypeCommonFile commonFile =
                        ArchetypeCommonFile.fromFile(sourceFile.getSourcePath().toString());
                if (commonFile != null) {
                    acc.addCommonFile(commonFile);
                    LOG.debug("File {} is a common file", sourceFile.getSourcePath());
                } else {
                    acc.addOtherFile(sourceFile.getSourcePath().toString());
                    LOG.debug("File {} is not a common file", sourceFile.getSourcePath());
                }
                groovyIsoVisitor.visit(tree, ctx);
                return tree;
            }

            final TreeVisitor<?, ExecutionContext> groovyIsoVisitor = Preconditions.check(
                    new FindSourceFiles("**/Jenkinsfile"), new GroovyIsoVisitor<ExecutionContext>() {
                        @Override
                        public J.MethodInvocation visitMethodInvocation(
                                J.MethodInvocation method, ExecutionContext ctx) {
                            J.MethodInvocation m = super.visitMethodInvocation(method, ctx);
                            if ("buildPlugin".equals(m.getSimpleName())) {
                                List<Expression> args = m.getArguments();

                                List<Integer> jdkVersions = args.stream()
                                        .filter(arg -> arg instanceof G.MapEntry)
                                        .map(G.MapEntry.class::cast)
                                        .filter(entry ->
                                                "configurations".equals(((J.Literal) entry.getKey()).getValue()))
                                        .flatMap(entry -> ((G.ListLiteral) entry.getValue()).getElements().stream())
                                        .filter(expression -> expression instanceof G.MapLiteral)
                                        .flatMap(expression -> ((G.MapLiteral) expression).getElements().stream())
                                        .filter(mapExpr -> mapExpr instanceof G.MapEntry)
                                        .map(G.MapEntry.class::cast)
                                        .filter(mapEntry -> "jdk".equals(((J.Literal) mapEntry.getKey()).getValue()))
                                        .map(mapEntry -> Integer.parseInt(((J.Literal) mapEntry.getValue())
                                                .getValue()
                                                .toString()))
                                        .distinct()
                                        .toList();

                                jdkVersions.forEach(jdkVersion -> acc.addJdk(JDK.get(jdkVersion)));
                            }
                            return m;
                        }
                    });
        };
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor(MetadataAccumulator acc) {
        return new MavenIsoVisitor<>() {
            @Override
            public Xml.Document visitDocument(Xml.Document document, ExecutionContext ctx) {

                // Ensure maven resolution result is present
                Markers markers = document.getMarkers();
                Optional<MavenResolutionResult> mavenResolutionResult = markers.findFirst(MavenResolutionResult.class);
                if (mavenResolutionResult.isEmpty()) {
                    return document;
                }

                // Get the pom
                MavenResolutionResult resolutionResult = mavenResolutionResult.get();
                ResolvedPom resolvedPom = resolutionResult.getPom();
                Pom pom = resolvedPom.getRequested();

                TagExtractor tagExtractor = new TagExtractor();
                tagExtractor.visit(document, ctx);

                // Store flags on the accumulator
                acc.addFlags(tagExtractor.getFlags());
                LOG.info("Flags detected: {}", acc.getFlags());

                // Remove the properties that are not needed and specific to the build environment
                Map<String, String> properties = pom.getProperties();
                properties.remove("project.basedir");
                properties.remove("basedir");

                // Construct the plugin metadata
                PluginMetadata pluginMetadata = new PluginMetadata();
                pluginMetadata.setPluginName(pom.getName());
                Parent parent = pom.getParent();
                if (parent != null) {
                    pluginMetadata.setParentVersion(parent.getVersion());
                }
                pluginMetadata.setProperties(properties);
                pluginMetadata.setJenkinsVersion(
                        resolvedPom.getManagedVersion("org.jenkins-ci.main", "jenkins-core", null, null));
                pluginMetadata.setFlags(acc.getFlags());
                pluginMetadata.setCommonFiles(acc.getCommonFiles());
                pluginMetadata.setOtherFiles(acc.getOtherFiles());
                pluginMetadata.setJdks(acc.getJdkVersions());

                // Write the metadata to a file for later use by the plugin modernizer.
                pluginMetadata.save();
                LOG.debug("Plugin metadata written to {}", pluginMetadata.getRelativePath());
                LOG.debug(JsonUtils.toJson(pluginMetadata));

                return document;
            }
        };
    }

    /**
     * Maven visitor to extract tags from pom.xml.
     */
    private static class TagExtractor extends MavenIsoVisitor<ExecutionContext> {

        /**
         * Detected flag
         */
        private final List<MetadataFlag> flags = new ArrayList<>();

        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag t = super.visitTag(tag, ctx);
            List<MetadataFlag> newFlags = Arrays.stream(MetadataFlag.values())
                    .filter(flag -> flag.isApplicable(tag))
                    .toList();
            flags.addAll(newFlags);
            if (!newFlags.isEmpty()) {
                LOG.debug(
                        "Flags detected for tag {} {}",
                        tag,
                        newFlags.stream().map(Enum::name).collect(Collectors.joining(", ")));
            }
            return t;
        }

        /**
         * Get the flags for this visitor.
         * @return flags for this visitor
         */
        public List<MetadataFlag> getFlags() {
            return flags;
        }
    }
}
