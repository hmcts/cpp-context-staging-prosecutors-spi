package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.SAME_SOURCE_AND_DESTINATION_SYSTEM;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.cjse.schemas.common.operations.RouteDataStreamType;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.vo.OIInputVO;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public class SameSourceAndDestinationOIValidationRule extends AbstractValidationRule implements OIValidationRule {

    public SameSourceAndDestinationOIValidationRule() {
        super(SAME_SOURCE_AND_DESTINATION_SYSTEM);
    }

    @Override
    public Optional<ValidationError> validate(final RouteDataRequestType routeDataRequestType, final OIInputVO oiInputVO) {

        final RouteDataRequestType.Routes routes = routeDataRequestType.getRoutes();
        if (routes == null) {
            return Optional.empty(); //ignore further checks
        }

        final List<RouteDataStreamType> routeList = routes.getRoute();
        if (routeList == null || routeList.isEmpty() || routeList.get(0).getRouteDestinationSystem() == null || routeList.get(0).getRouteSourceSystem() == null) {
            return Optional.empty(); //ignore further checks
        }

        final String inputRouteDestinationSystem = routeList.get(0).getRouteDestinationSystem().getValue();
        final String inputRouteSourceSystem = routeList.get(0).getRouteSourceSystem().getValue();

        if (StringUtils.isEmpty(inputRouteDestinationSystem) || StringUtils.isEmpty(inputRouteSourceSystem)) {
            return Optional.empty(); //ignore further checks
        }

        return !inputRouteSourceSystem.equals(inputRouteDestinationSystem) ? Optional.empty() : Optional.of(getValidationError());
    }

}
