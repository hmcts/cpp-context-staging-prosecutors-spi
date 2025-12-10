package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.containsWhitespace;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_REQUEST_ID;

import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;

import java.util.Optional;


public class RequestIdValidationRule extends AbstractValidationRule implements MDIValidationRule {


    public RequestIdValidationRule() {
        super(INVALID_REQUEST_ID);
    }

    @Override
    public Optional<ValidationError> validate(final SubmitRequest submitRequest) {
        final String requestID = submitRequest.getRequestID();
        return isEmpty(requestID) || containsWhitespace(requestID) ? of(getValidationError()) : empty();
    }
}
