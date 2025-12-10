package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import static java.util.Objects.isNull;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_DATA_CONTROLLER;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.cjse.schemas.common.operations.SystemDetailsStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.vo.OIInputVO;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;


public class InvalidDataControllerOIValidationRule extends AbstractValidationRule implements OIValidationRule {

    public InvalidDataControllerOIValidationRule() {
        super(INVALID_DATA_CONTROLLER);
    }


    @Override
    public Optional<ValidationError> validate(final RouteDataRequestType routeDataRequestType, final OIInputVO oi) {

        final SystemDetailsStructure.DataController dataController = routeDataRequestType.getRequestFromSystem().getDataController();
        if (isNull(dataController) || (!isEmpty(dataController.getValue()) && StringUtils.length(dataController.getValue()) <= 50)) {
            return Optional.empty();
        }
        return of(getValidationError());
    }

}
