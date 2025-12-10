package uk.gov.moj.cpp.staging.prosecutors.spi.utils;


import java.io.IOException;
import java.io.StringReader;
import java.net.URL;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

public class XmlSchemaValidator {

    private XmlSchemaValidator() {
    }

    @SuppressWarnings("squid:S2755")
    public static void validate(String xmlPayload, String schemaPath) throws IOException, SAXException {

        final URL schemaFile = Thread.currentThread().getContextClassLoader().getResource(schemaPath);
        final Source xmlFile = new StreamSource(new StringReader(xmlPayload));

        final SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        final Schema schema = schemaFactory.newSchema(schemaFile);

        schema.newValidator().validate(xmlFile);
    }
}