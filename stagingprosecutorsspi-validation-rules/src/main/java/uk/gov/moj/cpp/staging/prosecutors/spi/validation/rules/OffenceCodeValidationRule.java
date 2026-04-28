package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import static java.util.Optional.of;
import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.dca.xmlschemas.libra.StdProsPoliceNewCaseStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;
import uk.gov.moj.cpp.staging.soap.schema.ObjectUnMarshaller;

import java.util.Optional;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

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
        try {
            final Object requestOrResponseType = ((JAXBElement) new ObjectUnMarshaller().getRequestOrResponseType(input.getMessage())).getValue();
            if(requestOrResponseType instanceof RouteDataRequestType){
                final RouteDataRequestType requestType = (RouteDataRequestType) requestOrResponseType;
                final StdProsPoliceNewCaseStructure stdProsPoliceNewCaseStructure = new ObjectUnMarshaller().getStdProsPoliceNewCaseStructure(requestType.getDataStream().getDataStreamContent());
                if (stdProsPoliceNewCaseStructure.getCase().getDefendant().stream()
                        .flatMap(def -> def.getOffence().stream())
                        .anyMatch(off -> StringUtils.isBlank(off.getBaseOffenceDetails().getOffenceCode()))){
                    return of(getValidationError());
                }
            }
        } catch (JAXBException | SAXException e  ) {
            LOGGER.info("Input XML failed validation: {}", e.getCause());
        }
        return Optional.empty();
    }
}
