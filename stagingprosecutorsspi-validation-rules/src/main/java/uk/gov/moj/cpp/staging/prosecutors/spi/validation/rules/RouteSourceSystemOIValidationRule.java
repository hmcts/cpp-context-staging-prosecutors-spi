package uk.gov.moj.cpp.staging.prosecutors.spi.validation.rules;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError.INVALID_ROUTE_SOURCE_SYSTEM;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.cjse.schemas.common.operations.RouteDataStreamType;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SystemCodes;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.ValidationError;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.service.ReferenceDataService;
import uk.gov.moj.cpp.staging.prosecutors.spi.validation.vo.OIInputVO;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

public class RouteSourceSystemOIValidationRule extends AbstractValidationRule implements OIValidationRule {

    @Inject
    private ReferenceDataService referenceDataQueryService;

    @Inject
    public RouteSourceSystemOIValidationRule() {
        super(INVALID_ROUTE_SOURCE_SYSTEM);
    }

    @Override
    public Optional<ValidationError> validate(final RouteDataRequestType routeDataRequestType, final OIInputVO oiInputVO) {

        final RouteDataRequestType.Routes routes = routeDataRequestType.getRoutes();
        if(routes == null) {
            return Optional.of(getValidationError());
        }

        final List<RouteDataStreamType> routeList = routeDataRequestType.getRoutes().getRoute();
        if (routeList == null || routeList.isEmpty() || routeList.get(0).getRouteSourceSystem() == null) {
            return Optional.of(getValidationError());
        }

        final String inputRouteSourceSystem = routeList.get(0).getRouteSourceSystem().getValue();

        if (!StringUtils.isEmpty(inputRouteSourceSystem) && validSourceSystem(inputRouteSourceSystem)
                && validLength(inputRouteSourceSystem)) {
            return Optional.empty();
        }


        return Optional.of(getValidationError());
    }

    private boolean validSourceSystem(final String inputRouteSourceSystem) {
        final List<SystemCodes> systemCodesRefData = referenceDataQueryService.retrieveSystemCodes();
        if(systemCodesRefData==null || systemCodesRefData.isEmpty()) {
            return false;
        }

        final Optional<SystemCodes> systemCodeFoundOpt = systemCodesRefData.stream().filter(p -> p.getSystemCode().equals(inputRouteSourceSystem)).findAny();

        return systemCodeFoundOpt.isPresent() && systemCodeFoundOpt.get().getSpiInFlag();
    }

    private boolean validLength(final String inputRouteDestinationSystem) {

        return !isEmpty(inputRouteDestinationSystem) && StringUtils.length(inputRouteDestinationSystem) <= 50;

    }
}
