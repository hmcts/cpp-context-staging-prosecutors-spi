package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import static java.util.Objects.isNull;
import static java.util.Optional.of;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_ORGANIZATIONAL_UNIT_ID;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.vo.OIInputVO;

import java.util.Optional;
import java.util.regex.Pattern;


public class InvalidOrganizationalUnitIdOIValidationRule extends AbstractValidationRule implements OIValidationRule {

        public static final String PATTERN_7_8_ALPHANUMERIC = "^.{7,8}$";

        public InvalidOrganizationalUnitIdOIValidationRule() {
            super(INVALID_ORGANIZATIONAL_UNIT_ID);
        }


        @Override
        public Optional<ValidationError> validate(final RouteDataRequestType routeDataRequestType, final OIInputVO oi) {
            if (isNull(routeDataRequestType.getRequestFromSystem().getOrganizationalUnitID()) ) {
                return Optional.empty();
            }
            return Pattern.compile(PATTERN_7_8_ALPHANUMERIC).
                    matcher(routeDataRequestType.getRequestFromSystem().getOrganizationalUnitID().getValue()).matches() ? Optional.empty() : of(getValidationError());
        }
    }
