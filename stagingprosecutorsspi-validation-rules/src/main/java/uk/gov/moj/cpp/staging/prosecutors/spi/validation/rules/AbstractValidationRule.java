package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;

public class AbstractValidationRule {
    private final ValidationError validationError;

    public AbstractValidationRule(final ValidationError validationError) {
        this.validationError = validationError;
    }

    public int getErrorCode() {
        return validationError.getCode();
    }

    public ValidationError getValidationError() {
        return validationError;
    }
}
