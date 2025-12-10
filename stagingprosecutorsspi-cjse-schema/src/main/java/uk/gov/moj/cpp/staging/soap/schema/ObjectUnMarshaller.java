package uk.gov.moj.cpp.staging.soap.schema;

import static javax.xml.bind.JAXBContext.newInstance;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.dca.xmlschemas.libra.StdProsPoliceNewCaseStructure;

import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.xml.sax.SAXException;

public class ObjectUnMarshaller {

    private static final String STANDARD_POLICE_CASE_XSD = "xsd/CPP-StdProsPoliceCase-v1-2.xsd";


    @SuppressWarnings("squid:S1160")
    public StdProsPoliceNewCaseStructure getStdProsPoliceNewCaseStructure(final String payload) throws JAXBException, SAXException {

        return unmarshallValidateAndCreateObject(new StreamSource(new StringReader(payload)),
                ObjectUnMarshaller.STANDARD_POLICE_CASE_XSD, StdProsPoliceNewCaseStructure.class).getValue();

    }

    public RouteDataRequestType getRouteDataRequestType(final String payload) throws JAXBException {
        return newInstance(RouteDataRequestType.class).createUnmarshaller()
                .unmarshal(new StreamSource(new StringReader(payload)), RouteDataRequestType.class)
                .getValue();
    }


    public Object getRequestOrResponseType(final String payload) throws JAXBException {
        return newInstance("uk.gov.cjse.schemas.common.operations").createUnmarshaller()
                .unmarshal(new StreamSource(new StringReader(payload)));

    }

    private <T> JAXBElement<T> unmarshallValidateAndCreateObject(final javax.xml.transform.Source source, final String xsdSchema, final Class<T> declaredType)
            throws JAXBException, SAXException {

        final Unmarshaller unmarshaller = getUnmarshaller(declaredType);
        unmarshaller.setSchema(getSchema(xsdSchema));
        return unmarshaller.unmarshal(source, declaredType);
    }

    private <T> Unmarshaller getUnmarshaller(final Class<T> declaredType) throws JAXBException {
        return newInstance(declaredType).createUnmarshaller();
    }


    private Schema getSchema(final String xsdSchema) throws SAXException {
        final SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        return sf.newSchema(Thread.currentThread().getContextClassLoader().getResource(xsdSchema));
    }


}