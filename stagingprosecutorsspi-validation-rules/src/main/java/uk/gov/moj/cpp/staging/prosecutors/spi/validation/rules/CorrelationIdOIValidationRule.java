package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_CORRELATION_ID_CODE;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.vo.OIInputVO;

import java.util.Optional;
import java.util.regex.Pattern;


public class CorrelationIdOIValidationRule extends AbstractValidationRule implements OIValidationRule {

    public static final String PATTERN_1_50_ALPHANUMERIC = "^[\\S]{1,50}$";


    public CorrelationIdOIValidationRule() {
        super(INVALID_CORRELATION_ID_CODE);
    }


    @Override
    public Optional<ValidationError> validate(final RouteDataRequestType routeDataRequestType, final OIInputVO oiInputVO) {
        final String correlationId = routeDataRequestType.getRequestFromSystem().getCorrelationID();
        return Pattern.compile(PATTERN_1_50_ALPHANUMERIC).
                matcher(correlationId).matches() ?
                empty() : of(getValidationError());
    }
}
