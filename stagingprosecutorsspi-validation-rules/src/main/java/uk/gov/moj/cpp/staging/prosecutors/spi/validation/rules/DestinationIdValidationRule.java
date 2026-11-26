package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_DESTINATION_ID;

import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;


public class DestinationIdValidationRule extends AbstractValidationRule implements MDIValidationRule {
    @Inject
    @Value(key = "cpp.system.id.spi.in", defaultValue = "B00LIBRA")
    private String cppSystemId;

    @Inject
    @Value(key = "cpp.system.id", defaultValue = "C00CommonPlatform")
    private String cppAsCJSEDestination;

    @Inject
    public DestinationIdValidationRule() {
        super(INVALID_DESTINATION_ID);
    }

    @Override
    public Optional<ValidationError> validate(final SubmitRequest submitRequest) {
        final List<String> destinationIdList = submitRequest.getDestinationID();
        if (destinationIdList == null || destinationIdList.isEmpty()) {
            return of(getValidationError());
        }

        final Optional<String> result = destinationIdList.stream().filter(destinationId -> destinationId.equalsIgnoreCase(cppSystemId) || destinationId.equalsIgnoreCase(cppAsCJSEDestination)).findAny();
        return result.isPresent() ? empty() : of(getValidationError());
    }

}
