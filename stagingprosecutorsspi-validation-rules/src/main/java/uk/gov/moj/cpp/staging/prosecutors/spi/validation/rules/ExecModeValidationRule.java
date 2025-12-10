package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_EXEC_MODE;

import uk.gov.cjse.schemas.endpoint.types.ExecMode;
import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;

import java.util.Optional;

public class ExecModeValidationRule extends AbstractValidationRule implements MDIValidationRule {


    public ExecModeValidationRule() {
        super(INVALID_EXEC_MODE);
    }

    @Override
    public Optional<ValidationError> validate(final SubmitRequest submitRequest) {
        final ExecMode execMode = submitRequest.getExecMode();
        if (execMode == null) {
            return Optional.of(getValidationError());
        }
        return ExecMode.ASYNCH.equals(execMode) ? Optional.empty() : Optional.of(getValidationError());
    }
}
