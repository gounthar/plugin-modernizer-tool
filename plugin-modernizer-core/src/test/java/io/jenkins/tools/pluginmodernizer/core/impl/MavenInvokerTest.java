package io.jenkins.tools.pluginmodernizer.core.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import io.jenkins.tools.pluginmodernizer.core.config.Config;
import io.jenkins.tools.pluginmodernizer.core.model.Plugin;
import io.jenkins.tools.pluginmodernizer.core.utils.JdkFetcher;
import java.nio.file.Path;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MavenInvokerTest {

    @Mock
    private Config config;

    @Mock
    private JdkFetcher jdkFetcher;

    private MavenInvoker mavenInvoker;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mavenInvoker = new MavenInvoker(config, jdkFetcher);
    }

    @Test
    public void testAddRelativePathIfMissing() throws Exception {
        Plugin plugin = mock(Plugin.class);
        Path pomPath = Path.of("src/test/resources/pom.xml");
        when(plugin.getLocalRepository()).thenReturn(pomPath.getParent());

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(pomPath.toFile());

        Element parentElement = (Element) document.getElementsByTagName("parent").item(0);
        assertEquals(0, parentElement.getElementsByTagName("relativePath").getLength());

        mavenInvoker.addRelativePathIfMissing(plugin);

        document = builder.parse(pomPath.toFile());
        parentElement = (Element) document.getElementsByTagName("parent").item(0);
        assertEquals(1, parentElement.getElementsByTagName("relativePath").getLength());
        assertTrue(parentElement.getElementsByTagName("relativePath").item(0).getTextContent().contains("../pom.xml"));
    }
}
