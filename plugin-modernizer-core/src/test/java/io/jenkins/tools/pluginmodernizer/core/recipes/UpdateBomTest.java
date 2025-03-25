package io.jenkins.tools.pluginmodernizer.core.recipes;

import static org.openrewrite.maven.Assertions.pomXml;

import io.jenkins.tools.pluginmodernizer.core.utils.XmlTestDataLoader;
import java.io.IOException;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.openrewrite.Issue;
import org.openrewrite.maven.MavenParser;
import org.openrewrite.test.RewriteTest;

/**
 * Test for {@link UpdateBom}.
 */
@Execution(ExecutionMode.CONCURRENT)
public class UpdateBomTest implements RewriteTest {

    @Test
    void shouldSkipIfNoBom() throws IOException {
        String xmlTemplate = XmlTestDataLoader.loadXmlTemplate("src/test/resources/xml/base/pom.xml");
        Properties properties = XmlTestDataLoader.loadProperties("src/test/resources/properties/versions.properties");
        String customizedXml = XmlTestDataLoader.customizeXmlTemplate(xmlTemplate, properties);

        rewriteRun(
                spec -> spec.recipe(new UpdateBom()),
                // language=xml
                pomXml(customizedXml));
    }

    @Test
    void shouldUpdateToLatestReleasedWithoutMavenConfig() throws IOException {
        String xmlTemplate = XmlTestDataLoader.loadXmlTemplate("src/test/resources/xml/bom/bom-configuration.xml");
        Properties properties = XmlTestDataLoader.loadProperties("src/test/resources/properties/versions.properties");
        String customizedXml = XmlTestDataLoader.customizeXmlTemplate(xmlTemplate, properties);

        rewriteRun(
                spec -> spec.recipe(new UpdateBom()),
                // language=xml
                pomXml(customizedXml));
    }

    @Test
    @Issue("https://github.com/jenkins-infra/plugin-modernizer-tool/issues/534")
    void shouldUpdateToLatestReleasedWithIncrementalsEnabled() throws IOException {
        String xmlTemplate =
                XmlTestDataLoader.loadXmlTemplate("src/test/resources/xml/incrementals/incrementals-configuration.xml");
        Properties properties = XmlTestDataLoader.loadProperties("src/test/resources/properties/versions.properties");
        String customizedXml = XmlTestDataLoader.customizeXmlTemplate(xmlTemplate, properties);

        rewriteRun(
                spec -> {
                    spec.parser(MavenParser.builder().activeProfiles("consume-incrementals"));
                    spec.recipe(new UpdateBom());
                },
                // language=xml
                pomXml(customizedXml));
    }

    @Test
    void shouldUpdateToLatestIncrementalsWithoutMavenConfig() throws IOException {
        String xmlTemplate =
                XmlTestDataLoader.loadXmlTemplate("src/test/resources/xml/incrementals/incrementals-configuration.xml");
        Properties properties = XmlTestDataLoader.loadProperties("src/test/resources/properties/versions.properties");
        String customizedXml = XmlTestDataLoader.customizeXmlTemplate(xmlTemplate, properties);

        rewriteRun(
                spec -> spec.recipe(new UpdateBom()),
                // language=xml
                pomXml(customizedXml));
    }

    @Test
    void shouldSkipIfOnParentBom() throws IOException {
        String xmlTemplate = XmlTestDataLoader.loadXmlTemplate("src/test/resources/xml/base/pom.xml");
        Properties properties = XmlTestDataLoader.loadProperties("src/test/resources/properties/versions.properties");
        String customizedXml = XmlTestDataLoader.customizeXmlTemplate(xmlTemplate, properties);

        rewriteRun(
                spec -> spec.recipe(new UpdateBom()),
                // language=xml
                pomXml(customizedXml));
    }

    @Test
    void shouldUpgradePropertyForVersion() throws IOException {
        String xmlTemplate =
                XmlTestDataLoader.loadXmlTemplate("src/test/resources/xml/properties/properties-configuration.xml");
        Properties properties = XmlTestDataLoader.loadProperties("src/test/resources/properties/versions.properties");
        String customizedXml = XmlTestDataLoader.customizeXmlTemplate(xmlTemplate, properties);

        rewriteRun(
                spec -> spec.recipe(new UpdateBom()),
                // language=xml
                pomXml(customizedXml));
    }
}
