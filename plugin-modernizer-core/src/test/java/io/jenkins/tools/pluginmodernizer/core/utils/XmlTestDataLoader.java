package io.jenkins.tools.pluginmodernizer.core.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    /**
     * Customizes an XML template by replacing placeholders in the format ${key} with values from properties.
     *
     * @param xmlTemplate The XML template containing placeholders
     * @param properties  The properties containing replacement values
     * @return The customized XML string
     * @throws IllegalArgumentException if xmlTemplate or properties is null
     */
    public static String customizeXmlTemplate(String xmlTemplate, Properties properties) {
        if (xmlTemplate == null) {
            throw new IllegalArgumentException("XML template cannot be null");
        }
        if (properties == null) {
            throw new IllegalArgumentException("Properties cannot be null");
        }
        StringBuilder result = new StringBuilder(xmlTemplate);

        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            if (value != null) {
                int start;
                String placeholder = "${" + key + "}";
                while ((start = result.indexOf(placeholder)) != -1) {
                    result.replace(start, start + placeholder.length(), value);
                }
            }
        }
        return result.toString();
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
