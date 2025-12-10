package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;

import java.util.Optional;

public interface ValidationRuleWithVO<T, E> {

    Optional<ValidationError> validate(final T input, final E oiInputVO);

    int getErrorCode();

}
