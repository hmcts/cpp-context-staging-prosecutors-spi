package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_ROUTE_DESTINATION_SYSTEM;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.cjse.schemas.common.operations.RouteDataStreamType;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.vo.OIInputVO;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class RouteDestinationSystemOIValidationRule extends AbstractValidationRule implements OIValidationRule {

    @Inject
    @Value(key = "cpp.system.id.spi.in", defaultValue = "C00CommonPlatform")
    private String routeDestinationSystem;

    @Inject
    public RouteDestinationSystemOIValidationRule() {
        super(INVALID_ROUTE_DESTINATION_SYSTEM);
    }

    @Override
    public Optional<ValidationError> validate(final RouteDataRequestType routeDataRequestType, final OIInputVO oiInputVO) {
        final RouteDataRequestType.Routes routes = routeDataRequestType.getRoutes();
        if (routes == null) {
            return Optional.of(getValidationError());
        }

        final List<RouteDataStreamType> routeList = routes.getRoute();
        if (routeList == null || routeList.isEmpty() || routeList.get(0).getRouteDestinationSystem() == null) {
            return Optional.of(getValidationError());
        }

        final String inputRouteDestinationSystem = routeList.get(0).getRouteDestinationSystem().getValue();

        if (!StringUtils.isEmpty(routeDestinationSystem) && inputRouteDestinationSystem.equals(routeDestinationSystem)
                && validLength(routeDestinationSystem)) {
            return Optional.empty();
        }

        return Optional.of(getValidationError());
    }

    private boolean validLength(final String inputRouteDestinationSystem) {

        return !isEmpty(inputRouteDestinationSystem) && StringUtils.length(inputRouteDestinationSystem) <= 50;

    }
}
