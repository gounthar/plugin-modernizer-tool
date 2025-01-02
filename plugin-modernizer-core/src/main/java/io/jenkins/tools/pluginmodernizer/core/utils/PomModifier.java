package io.jenkins.tools.pluginmodernizer.core.utils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.*;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Utility class for modifying POM files.
 */
public class PomModifier {

    private static final Logger LOG = LoggerFactory.getLogger(PomModifier.class);
    private Document document;
    private final Path pomFilePath;
    private final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    private final XPath xPath = XPathFactory.newInstance().newXPath();
    private final javax.xml.xpath.XPathExpression packageXPath;
    private final javax.xml.xpath.XPathExpression artifactIdXPath;

    /**
     * Constructor for PomModifier.
     *
     * @param pomFilePath the path to the POM file
     * @throws IllegalArgumentException if the file path is invalid
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public PomModifier(String pomFilePath) throws XPathExpressionException {
        packageXPath = xPath.compile("/project/packaging");
        artifactIdXPath = xPath.compile("/project/artifactId");
        try {
            // Validate the file path
            this.pomFilePath = Paths.get(pomFilePath).normalize().toAbsolutePath();
            if (!Files.exists(this.pomFilePath) || !Files.isRegularFile(this.pomFilePath)) {
                throw new IllegalArgumentException("Invalid file path: " + this.pomFilePath);
            } else if (!Files.isReadable(this.pomFilePath)) {
                throw new IllegalArgumentException("File is not readable: " + this.pomFilePath);
            }

            File pomFile = this.pomFilePath.toFile();
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
     * Return the packaging type of the POM file.
     *
     * @return the packaging type or null if not found
     */
    public String getPackaging() {
        if (document == null) {
            LOG.warn("Document is null for {}", pomFilePath);
            return null;
        }
        try {
            String packaging = packageXPath.evaluate(document);
            return packaging.isEmpty() ? null : packaging;
        } catch (Exception e) {
            LOG.warn("Error getting packaging from {}: {}", pomFilePath, e.getMessage());
            return null;
        }
    }

    /**
     * Return the groupId of the POM file.
     *
     * @return the groupId or null if not found
     */
    public String getArtifactId() {
        if (document == null) {
            LOG.warn("Document is null for {}", pomFilePath);
            return null;
        }
        try {
            return artifactIdXPath.evaluate(document);
        } catch (Exception e) {
            LOG.warn("Error getting artifactId from {}: {}", pomFilePath, e.getMessage());
            return null;
        }
    }

    private List<Node> getPrecedingComments(NodeList childNodes, int currentIndex) {
        List<Node> comments = new ArrayList<>();
        int j = currentIndex - 1;
        while (j >= 0) {
            Node previousNode = childNodes.item(j);
            if (isCommentOrWhitespace(previousNode)) {
                comments.add(previousNode);
                j--;
            } else {
                break;
            }
        }
        return comments;
    }

    private boolean isCommentOrWhitespace(Node node) {
        return node.getNodeType() == Node.COMMENT_NODE
                || (node.getNodeType() == Node.TEXT_NODE
                        && node.getTextContent().trim().startsWith("<!--"))
                || node.getTextContent().replaceAll("\\s+", "").isEmpty();
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
                        nodesToRemove.addAll(getPrecedingComments(childNodes, i));
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
     * @return boolean Returns true if at least one URL was changed, false otherwise.
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
     * Adds a self-closing relativePath tag to the parent tag in the POM file using a STAX parser.
     */
    public void addRelativePath() {
        try {
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            // Disable external entity processing to prevent XXE attacks
            inputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            inputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            inputFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            // Preserve CDATA and comments
            inputFactory.setProperty(XMLInputFactory.IS_COALESCING, false);
            inputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
            XMLEventReader reader = inputFactory.createXMLEventReader(Files.newInputStream(pomFilePath));
            StringWriter stringWriter = new StringWriter();
            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            XMLEventWriter writer = outputFactory.createXMLEventWriter(stringWriter);
            boolean parentTagOpen = false;
            boolean relativePathAdded = false;
            XMLEventFactory eventFactory = XMLEventFactory.newInstance();
            while (reader.hasNext()) {
                XMLEvent event = reader.nextEvent();
                if (event.isStartElement()
                        && event.asStartElement().getName().getLocalPart().equals("parent")) {
                    parentTagOpen = true;
                }
                if (parentTagOpen
                        && event.isEndElement()
                        && event.asEndElement().getName().getLocalPart().equals("parent")) {
                    if (!relativePathAdded) {
                        // Add newline and indentation
                        writer.add(eventFactory.createCharacters("  "));
                        StartElement startElement = eventFactory.createStartElement("", "", "relativePath");
                        writer.add(startElement);
                        writer.add(eventFactory.createEndElement("", "", "relativePath"));
                        // Add newline and indentation
                        writer.add(eventFactory.createCharacters("\n  "));
                        relativePathAdded = true;
                    }
                    parentTagOpen = false;
                }
                writer.add(event);
            }
            writer.close();
            reader.close();

            // Secure DocumentBuilderFactory configuration
            dbFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbFactory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbFactory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbFactory.setXIncludeAware(false);
            dbFactory.setExpandEntityReferences(false);

            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            document = dBuilder.parse(
                    new ByteArrayInputStream(stringWriter.toString().getBytes(StandardCharsets.UTF_8)));
            Files.write(pomFilePath, stringWriter.toString().getBytes(StandardCharsets.UTF_8));
        } catch (XMLStreamException | IOException | ParserConfigurationException e) {
            String errorMessage =
                    String.format("Failed to add relativePath tag to %s: %s", pomFilePath, e.getMessage());
            LOG.error(errorMessage, e);
            throw new RuntimeException(errorMessage, e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
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
            XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();
            XMLEventWriter writer = outputFactory.createXMLEventWriter(
                    Files.newOutputStream(Paths.get(outputPath)), StandardCharsets.UTF_8.name());
            XMLEventFactory eventFactory = XMLEventFactory.newInstance();

            writer.add(eventFactory.createStartDocument(StandardCharsets.UTF_8.name(), "1.0"));
            writeNode(document.getDocumentElement(), writer, eventFactory);
            writer.add(eventFactory.createEndDocument());

            writer.close();
        } catch (XMLStreamException | IOException e) {
            LOG.error("Error saving POM file: " + e.getMessage(), e);
            throw new RuntimeException("Failed to save POM file", e);
        }
    }

    /**
     * Writes a DOM Node and its children to an XMLEventWriter.
     *
     * @param node         the DOM Node to write
     * @param writer       the XMLEventWriter to write to
     * @param eventFactory the XMLEventFactory to create XML events
     * @throws XMLStreamException if an error occurs while writing the XML
     */
    private void writeNode(Node node, XMLEventWriter writer, XMLEventFactory eventFactory) throws XMLStreamException {
        if (node == null) {
            LOG.warn("Attempted to write null node");
            return;
        }
        switch (node.getNodeType()) {
            case Node.ELEMENT_NODE:
                Element element = (Element) node;
                writer.add(eventFactory.createStartElement("", "", element.getTagName()));

                // Write attributes
                for (int i = 0; i < element.getAttributes().getLength(); i++) {
                    Node attr = element.getAttributes().item(i);
                    writer.add(eventFactory.createAttribute(attr.getNodeName(), attr.getNodeValue()));
                }

                // Write child nodes
                NodeList children = element.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    writeNode(children.item(i), writer, eventFactory);
                }

                writer.add(eventFactory.createEndElement("", "", element.getTagName()));
                break;

            case Node.TEXT_NODE:
                writer.add(eventFactory.createCharacters(node.getNodeValue()));
                break;

            case Node.COMMENT_NODE:
                writer.add(eventFactory.createComment(node.getNodeValue()));
                break;

            default:
                LOG.warn("Encountered unexpected node type: {}", node.getNodeType());
                break;
        }
    }
}
