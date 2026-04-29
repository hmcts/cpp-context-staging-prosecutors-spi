package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import static java.util.Optional.of;
import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.dca.xmlschemas.libra.StdProsPoliceNewCaseStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;
import uk.gov.moj.cpp.staging.soap.schema.ObjectUnMarshaller;

import java.io.StringReader;
import java.util.Optional;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xml.sax.SAXException;

public class OffenceCodeValidationRule extends AbstractValidationRule implements MDIValidationRule {

    private static final Logger LOGGER = getLogger(OffenceCodeValidationRule.class);

    public OffenceCodeValidationRule() {
        super(ValidationError.INVALID_OFFENCE_CODE);
    }

    @Override
    public Optional<ValidationError> validate(final SubmitRequest input) {

            XMLInputFactory factory = XMLInputFactory.newInstance();

            try (StringReader reader = new StringReader(input.getMessage())) {
                XMLStreamReader xmlReader = factory.createXMLStreamReader(reader);

                while (xmlReader.hasNext()) {
                    int event = xmlReader.next();

                    if (event == XMLStreamConstants.START_ELEMENT &&
                            "DataStreamContent".equals(xmlReader.getLocalName())) {

                        String innerXml = xmlReader.getElementText();

                        if (hasBlankOffenceCode(innerXml)) {
                            return of(getValidationError());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.info("Input XML failed validation: {}", e.getCause());
            }

        return Optional.empty();
    }


    public boolean hasBlankOffenceCode(String xml) {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        try (StringReader reader = new StringReader(xml)) {
            XMLStreamReader xmlReader = factory.createXMLStreamReader(reader);

            while (xmlReader.hasNext()) {
                int event = xmlReader.next();

                if (event == XMLStreamConstants.START_ELEMENT &&
                        "OffenceCode".equals(xmlReader.getLocalName())) {

                    String value = xmlReader.getElementText();

                    if (value == null || value.trim().isEmpty()) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.info("Input XML failed validation: {}", e.getCause());
        }

        return false;
    }
}
