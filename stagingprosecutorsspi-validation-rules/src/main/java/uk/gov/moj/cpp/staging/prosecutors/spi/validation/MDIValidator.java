package uk.gov.moj.cpp.staging.prosecutors.spi.validation;


import static java.util.Optional.empty;
import static java.util.stream.StreamSupport.stream;

import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.MDIValidationRule;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

public class MDIValidator {

    @Inject
    Instance<MDIValidationRule> validatorRules;

    public Optional<ValidationError> validate(final SubmitRequest submitRequestMes) {

        final Stream<MDIValidationRule> validationRuleStream = stream(validatorRules.spliterator(), false);
        final Optional<Optional<ValidationError>> validationErrorOptional = validationRuleStream
                .sorted(Comparator.comparing(MDIValidationRule::getErrorCode))
                .map(rule -> rule.validate(submitRequestMes))
                .filter(Optional::isPresent)
                .findFirst();

        return validationErrorOptional.isPresent() ? validationErrorOptional.get() : empty();

    }

}
