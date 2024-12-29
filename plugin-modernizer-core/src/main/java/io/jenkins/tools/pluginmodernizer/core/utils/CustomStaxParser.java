package io.jenkins.tools.pluginmodernizer.core.utils;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.io.OutputStream;

public class CustomStaxParser {

    private final XMLInputFactory xmlInputFactory;
    private final XMLOutputFactory xmlOutputFactory;

    public CustomStaxParser() {
        this.xmlInputFactory = XMLInputFactory.newInstance();
        this.xmlOutputFactory = XMLOutputFactory.newInstance();
    }

    public void parse(InputStream inputStream, OutputStream outputStream) throws Exception {
        XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(inputStream);
        XMLEventWriter xmlEventWriter = xmlOutputFactory.createXMLEventWriter(outputStream);

        while (xmlEventReader.hasNext()) {
            XMLEvent xmlEvent = xmlEventReader.nextEvent();
            xmlEventWriter.add(xmlEvent);
        }

        xmlEventReader.close();
        xmlEventWriter.close();
    }
}
