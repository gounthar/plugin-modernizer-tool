package io.jenkins.tools.pluginmodernizer.core.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class XmlTestDataLoader {

    public static String loadXmlTemplate(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
    }

public static String customizeXmlTemplate(String xmlTemplate, Properties properties) {
    if (xmlTemplate == null) {
        throw new IllegalArgumentException("XML template cannot be null");
    }
    if (properties == null) {
        throw new IllegalArgumentException("Properties cannot be null");
    }
    for (String key : properties.stringPropertyNames()) {
        String value = properties.getProperty(key);
        if (value != null) {
            xmlTemplate = xmlTemplate.replace("${" + key + "}", value);
        }
    }
    return xmlTemplate;
}

    public static Properties loadProperties(String filePath) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        Properties properties = new Properties();
        try (var input = Files.newInputStream(Paths.get(filePath))) {
            properties.load(input);
        }
        return properties;
    }
}
