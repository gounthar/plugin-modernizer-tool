package io.jenkins.tools.pluginmodernizer.core.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility class for modifying POM files.
 */
public class PomModifier {

    private static final Logger LOG = LoggerFactory.getLogger(PomModifier.class);
    private final Document document;

    /**
     * Constructor for PomModifier.
     *
     * @param pomFilePath the path to the POM file
     * @throws IllegalArgumentException if the file path is invalid
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public PomModifier(String pomFilePath) {
        try {
            // Validate the file path
            Path path = Paths.get(pomFilePath).normalize().toAbsolutePath();
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                throw new IllegalArgumentException("Invalid file path: " + path);
            }

            File pomFile = path.toFile();
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbFactory.setXIncludeAware(false);
            dbFactory.setExpandEntityReferences(false);
            // Ignore whitespace
            dbFactory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            document = dBuilder.parse(pomFile);
            document.getDocumentElement().normalize();
        } catch (InvalidPathException e) {
            LOG.error("Invalid file path: " + e.getMessage());
            throw new IllegalArgumentException("Invalid file path: " + pomFilePath, e);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid file path: " + e.getMessage());
            throw e; // Re-throw to ensure the caller is aware of the issue
        } catch (Exception e) {
            LOG.error("Error initializing PomModifier: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize PomModifier", e); // Re-throw as a runtime exception
        }
    }

    /**
     * Removes offending properties from the POM file.
     */
    public void removeOffendingProperties() {
        NodeList propertiesList = document.getElementsByTagName("properties");
        if (propertiesList.getLength() > 0) {
            Node propertiesNode = propertiesList.item(0);
            NodeList childNodes = propertiesNode.getChildNodes();
            List<Node> nodesToRemove = new ArrayList<>();

            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    String nodeName = node.getNodeName();
                    if (nodeName.equals("jenkins-test-harness.version") || nodeName.equals("java.level")) {
                        // Add the offending property to the list
                        nodesToRemove.add(node);

                        // Add preceding comments to the list
                        int j = i - 1;
                        while (j >= 0) {
                            Node previousNode = childNodes.item(j);
                            if (previousNode.getNodeType() == Node.COMMENT_NODE
                                    || (previousNode.getNodeType() == Node.TEXT_NODE
                                            && previousNode
                                                    .getTextContent()
                                                    .trim()
                                                    .startsWith("<!--"))
                                    || previousNode
                                            .getTextContent()
                                            .replaceAll("\\s+", "")
                                            .isEmpty()) {
                                nodesToRemove.add(previousNode);
                                j--;
                            } else {
                                break; // Stop if a non-comment node is encountered
                            }
                        }
                    }
                }
            }

            // Remove collected nodes
            for (Node nodeToRemove : nodesToRemove) {
                propertiesNode.removeChild(nodeToRemove);
            }
        }
    }

    /**
     * Updates the parent POM information.
     *
     * @param groupId    the groupId to set
     * @param artifactId the artifactId to set
     * @param version    the version to set
     */
    public void updateParentPom(String groupId, String artifactId, String version) {
        NodeList parentList = document.getElementsByTagName("parent");
        if (parentList.getLength() > 0) {
            Node parentNode = parentList.item(0);
            NodeList childNodes = parentNode.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    switch (node.getNodeName()) {
                        case "groupId":
                            node.setTextContent(groupId);
                            break;
                        case "artifactId":
                            node.setTextContent(artifactId);
                            break;
                        case "version":
                            node.setTextContent(version);
                            break;
                        default:
                            LOG.warn("Unexpected element in parent POM: " + node.getNodeName());
                            break;
                    }
                }
            }
        } else {
            Element parentElement = document.createElement("parent");

            Element groupIdElement = document.createElement("groupId");
            groupIdElement.appendChild(document.createTextNode(groupId));
            parentElement.appendChild(groupIdElement);

            Element artifactIdElement = document.createElement("artifactId");
            artifactIdElement.appendChild(document.createTextNode(artifactId));
            parentElement.appendChild(artifactIdElement);

            Element versionElement = document.createElement("version");
            versionElement.appendChild(document.createTextNode(version));
            parentElement.appendChild(versionElement);

            document.getDocumentElement().appendChild(parentElement);
        }
    }

    /**
     * Updates the Jenkins minimal version in the POM file.
     *
     * @param version the version to set
     */
    public void updateJenkinsMinimalVersion(String version) {
        NodeList propertiesList = document.getElementsByTagName("properties");
        if (propertiesList.getLength() > 0) {
            Node propertiesNode = propertiesList.item(0);
            NodeList childNodes = propertiesNode.getChildNodes();
            boolean versionUpdated = false;
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE
                        && node.getNodeName().equals("jenkins.version")) {
                    node.setTextContent(version);
                    versionUpdated = true;
                    break;
                }
            }
            if (!versionUpdated) {
                Element jenkinsVersionElement = document.createElement("jenkins.version");
                jenkinsVersionElement.appendChild(document.createTextNode(version));
                propertiesNode.appendChild(jenkinsVersionElement);
            }
        }
    }

    /**
     * Adds a BOM section to the POM file.
     *
     * @param groupId    the groupId of the BOM
     * @param artifactId the artifactId of the BOM
     * @param version    the version of the BOM
     */
    public void addBom(String groupId, String artifactId, String version) {
        NodeList dependencyManagementList = document.getElementsByTagName("dependencyManagement");
        Element dependencyManagementElement;

        if (dependencyManagementList.getLength() > 0) {
            dependencyManagementElement = (Element) dependencyManagementList.item(0);
        } else {
            dependencyManagementElement = document.createElement("dependencyManagement");
            document.getDocumentElement().appendChild(dependencyManagementElement);
        }

        Element dependenciesElement = (Element)
                dependencyManagementElement.getElementsByTagName("dependencies").item(0);
        if (dependenciesElement == null) {
            dependenciesElement = document.createElement("dependencies");
            dependencyManagementElement.appendChild(dependenciesElement);
        }

        Element dependencyElement = document.createElement("dependency");

        Element groupIdElement = document.createElement("groupId");
        groupIdElement.appendChild(document.createTextNode(groupId));
        dependencyElement.appendChild(groupIdElement);

        Element artifactIdElement = document.createElement("artifactId");
        artifactIdElement.appendChild(document.createTextNode(artifactId));
        dependencyElement.appendChild(artifactIdElement);

        Element versionElement = document.createElement("version");
        versionElement.appendChild(document.createTextNode(version));
        dependencyElement.appendChild(versionElement);

        Element typeElement = document.createElement("type");
        typeElement.appendChild(document.createTextNode("pom"));
        dependencyElement.appendChild(typeElement);

        Element scopeElement = document.createElement("scope");
        scopeElement.appendChild(document.createTextNode("import"));
        dependencyElement.appendChild(scopeElement);

        dependenciesElement.appendChild(dependencyElement);
    }

    /**
     * Replaces 'http' with 'https' in repository URLs.
     * <p>
     * This method iterates through all the url elements in the POM file and replaces
     * any URLs that start with 'http://' with 'https://'. This is useful for ensuring
     * that all repository URLs use a secure connection.
     *
     * @return false Always returns false, indicating that no specific condition is met.
     */
    public boolean replaceHttpWithHttps() {
        boolean changedAtLeastOneUrl = false;
        NodeList repositoryUrls = document.getElementsByTagName("url");
        for (int i = 0; i < repositoryUrls.getLength(); i++) {
            Node urlNode = repositoryUrls.item(i);
            String url = urlNode.getTextContent();
            if (url.startsWith("http://")) {
                urlNode.setTextContent(url.replace("http://", "https://"));
                changedAtLeastOneUrl = true;
            }
        }
        return changedAtLeastOneUrl;
    }

    /**
     * Adds a self-closing relativePath tag to the parent tag in the POM file.
     */
    public void addRelativePath() {
        NodeList parentList = document.getElementsByTagName("parent");
        if (parentList.getLength() == 0) {
            LOG.warn("No parent tag found in POM file");
            return;
        }

        try {
            Node parentNode = parentList.item(0);
            NodeList childNodes = parentNode.getChildNodes();
            boolean relativePathExists = false;
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node node = childNodes.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE
                        && node.getNodeName().equals("relativePath")) {
                    relativePathExists = true;
                    LOG.debug("relativePath tag already exists");
                    break;
                }
            }
            if (!relativePathExists) {
                Element relativePathElement = document.createElement("relativePath");
                parentNode.appendChild(relativePathElement);
                LOG.debug("Added relativePath tag to parent");
            }
        } catch (Exception e) {
            LOG.error("Error adding relativePath tag: " + e.getMessage(), e);
            throw new RuntimeException("Failed to add relativePath tag", e);
        }
    }

    /**
     * Saves the modified POM file to the specified output path.
     *
     * @param outputPath the path to save the POM file
     * @throws IllegalArgumentException if the output path is invalid
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public void savePom(String outputPath) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
            transformerFactory.setAttribute("indent-number", 2);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new File(outputPath));
            transformer.transform(source, result);
        } catch (InvalidPathException e) {
            LOG.error("Invalid output path: " + e.getMessage());
            throw new IllegalArgumentException("Invalid output path: " + outputPath, e);
        } catch (Exception e) {
            LOG.error("Error saving POM file: " + e.getMessage(), e);
            throw new RuntimeException("Failed to save POM file", e);
        }
    }
}
