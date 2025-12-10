package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.DISALLOWED_DATA_STREAM_TYPE;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.vo.OIInputVO;

import java.util.Optional;

public class DataStreamTypeOIValidationRule extends AbstractValidationRule implements OIValidationRule {

    private static final String ALLOWED_DATA_STREAM_VALUE = "SPINewCase";

    public DataStreamTypeOIValidationRule() {
        super(DISALLOWED_DATA_STREAM_TYPE);
    }

    @Override
    public Optional<ValidationError> validate(final RouteDataRequestType routeDataRequestType, final OIInputVO oiInputVO) {
        final String inputDataStreamType = routeDataRequestType.getDataStream().getDataStreamType().getValue();
        if (!inputDataStreamType.equals(ALLOWED_DATA_STREAM_VALUE)) {
            return of(getValidationError());
        }

        return Optional.empty();
    }
}
