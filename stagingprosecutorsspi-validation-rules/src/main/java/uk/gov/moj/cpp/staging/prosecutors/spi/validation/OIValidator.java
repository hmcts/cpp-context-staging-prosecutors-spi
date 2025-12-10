package uk.gov.moj.cpp.staging.prosecutors.spi.validation;


import static java.util.stream.StreamSupport.stream;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules.OIValidationRule;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.vo.OIInputVO;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;

public class OIValidator {

    @Inject
    Instance<OIValidationRule> validatorRules;

    public List<Optional<ValidationError>> validate(final RouteDataRequestType routeDataRequestType, final OIInputVO oiInputVO) {

        final Stream<OIValidationRule> validationRuleStream = stream(validatorRules.spliterator(), false);
        return validationRuleStream
                .sorted(Comparator.comparing(OIValidationRule::getErrorCode))
                .map(rule -> rule.validate(routeDataRequestType, oiInputVO))
                .collect(Collectors.toList());
    }

}
