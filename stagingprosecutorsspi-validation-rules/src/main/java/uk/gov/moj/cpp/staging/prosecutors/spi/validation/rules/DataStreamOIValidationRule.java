package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import static java.util.Optional.empty;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.UNUSABLE_DATA_STREAM;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.vo.OIInputVO;
import uk.gov.moj.cpp.staging.soap.schema.ObjectUnMarshaller;

import java.util.Optional;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.xml.sax.SAXException;

public class DataStreamOIValidationRule extends AbstractValidationRule implements OIValidationRule {
    private static final Logger LOGGER = getLogger(DataStreamOIValidationRule.class);

    @Inject
    public DataStreamOIValidationRule() {
        super(UNUSABLE_DATA_STREAM);
    }

    @Override
    @SuppressWarnings("squid:S1166")
    public Optional<ValidationError> validate(final RouteDataRequestType routeDataRequestType, final OIInputVO oiInputVO) {
        try {
            new ObjectUnMarshaller().getStdProsPoliceNewCaseStructure(routeDataRequestType.getDataStream().getDataStreamContent());
        } catch (JAXBException | SAXException e) {
            LOGGER.info("Input XML failed validation: {}", e.getCause());
            return Optional.of(getValidationError());
        }
        return empty();
    }


}
