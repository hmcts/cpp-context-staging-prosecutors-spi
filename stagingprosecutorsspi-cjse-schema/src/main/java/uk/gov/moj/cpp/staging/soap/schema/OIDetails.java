package uk.gov.moj.cpp.staging.soap.schema;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.cjse.schemas.common.operations.RouteDataResponseType;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class OIDetails {

    private UUID messageId;
    private RouteDataRequestType routeDataRequestType;
    private RouteDataResponseType routeDataResponseType;

    public OIDetails() {
    }

    public OIDetails(final UUID messageId, final RouteDataRequestType routeDataRequestType) {
        this.messageId = messageId;
        this.routeDataRequestType = routeDataRequestType;
    }

    public OIDetails(final UUID messageId, final RouteDataResponseType routeDataRequestType) {
        this.messageId = messageId;
        this.routeDataResponseType = routeDataRequestType;
    }

    public RouteDataRequestType getRouteDataRequestType() {
        return routeDataRequestType;
    }

    public RouteDataResponseType getRouteDataResponseType() {
        return routeDataResponseType;
    }

    public UUID getMessageId() {
        return messageId;
    }

    @JsonIgnore
    public boolean isForRequest() {
        return routeDataRequestType != null;
    }
}
