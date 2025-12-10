package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import static java.util.Optional.of;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_PREVIOUSLY_SENT_REQUEST;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.vo.OIInputVO;

import java.util.Optional;


public class PreviouslySentOIValidationRule extends AbstractValidationRule implements OIValidationRule {


    public PreviouslySentOIValidationRule() {
        super(INVALID_PREVIOUSLY_SENT_REQUEST);
    }


    @Override
    public Optional<ValidationError> validate(final RouteDataRequestType routeDataRequestType, final OIInputVO oiInputVO) {
        if (null == oiInputVO.getCorrelationId() && null == oiInputVO.getSystemId()) {
            return Optional.empty();
        } else {
            if (oiInputVO.getCorrelationId().equalsIgnoreCase(routeDataRequestType.getRequestFromSystem().getCorrelationID()) &&
                    oiInputVO.getSystemId().equalsIgnoreCase(routeDataRequestType.getRequestFromSystem().getSystemID().getValue())) {
                return of(getValidationError());
            } else {
                return Optional.empty();
            }
        }
    }
}
