package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;

import java.util.Optional;

public interface ValidationRule<T> {

    Optional<ValidationError> validate(final T input);

    int getErrorCode();

}
