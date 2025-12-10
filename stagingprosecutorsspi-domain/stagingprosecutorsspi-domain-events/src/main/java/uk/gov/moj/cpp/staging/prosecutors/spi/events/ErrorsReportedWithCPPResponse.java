package uk.gov.moj.cpp.staging.prosecutors.spi.events;

import uk.gov.cjse.schemas.common.operations.RouteDataResponseType;
import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("stagingprosecutorsspi.event.errors-reported-with-cpp-response")
public class ErrorsReportedWithCPPResponse {


    private final UUID messageId;

    private final RouteDataResponseType routeDataResponseType;

    @JsonCreator
    public ErrorsReportedWithCPPResponse(final UUID messageId, final RouteDataResponseType routeDataResponseType) {
        this.messageId = messageId;
        this.routeDataResponseType = routeDataResponseType;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public RouteDataResponseType getRouteDataResponseType() {
        return routeDataResponseType;
    }
}
