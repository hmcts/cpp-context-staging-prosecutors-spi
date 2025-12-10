package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;


import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_SOURCE_ID;

import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;

import java.util.Optional;

import javax.inject.Inject;

public class SourceIdValidationRule extends AbstractValidationRule implements MDIValidationRule {

    private String cjseSystemId;

    @Inject
    public SourceIdValidationRule(@Value(key = "cjse.system.id") String cjseSystemId) {
        super(INVALID_SOURCE_ID);
        this.cjseSystemId = cjseSystemId;
    }

    @Override
    public Optional<ValidationError> validate(final SubmitRequest submitRequest) {
        final String inputSourceId = submitRequest.getSourceID();

        if (inputSourceId == null || inputSourceId.isEmpty()) {
            return Optional.of(getValidationError());
        }
        return cjseSystemId.equalsIgnoreCase(inputSourceId) ? Optional.empty() : Optional.of(getValidationError());
    }
}
