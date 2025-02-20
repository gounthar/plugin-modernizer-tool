package io.jenkins.tools.pluginmodernizer.core.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class XmlTestDataLoader {

    public static String loadXmlTemplate(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)));
    }

    public static String customizeXmlTemplate(String xmlTemplate, Properties properties) {
        for (String key : properties.stringPropertyNames()) {
            xmlTemplate = xmlTemplate.replace("${" + key + "}", properties.getProperty(key));
        }
        return xmlTemplate;
    }

    public static Properties loadProperties(String filePath) throws IOException {
        Properties properties = new Properties();
        properties.load(Files.newInputStream(Paths.get(filePath)));
        return properties;
    }
}
