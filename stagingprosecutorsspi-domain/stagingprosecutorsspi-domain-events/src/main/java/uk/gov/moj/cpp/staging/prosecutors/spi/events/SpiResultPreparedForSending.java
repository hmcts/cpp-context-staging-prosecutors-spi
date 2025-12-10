package uk.gov.moj.cpp.staging.prosecutors.spi.events;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("stagingprosecutorsspi.event.spi-result-prepared-for-sending")
public class SpiResultPreparedForSending {

    private final String payload;

    private final String ptiUrn;

    private final UUID spiResultId;

    private final RouteDataRequestType routeDataRequestType;

    private final UUID messageId;


    @JsonCreator
    public SpiResultPreparedForSending(final RouteDataRequestType routeDataRequestType, final String payload, final String ptiUrn, final UUID spiResultId, final UUID messageId) {
        this.routeDataRequestType = routeDataRequestType;
        this.payload = payload;
        this.ptiUrn = ptiUrn;
        this.spiResultId = spiResultId;
        this.messageId = messageId;
    }

    public RouteDataRequestType getRouteDataRequestType() {
        return routeDataRequestType;
    }

    public String getPayload() {
        return payload;
    }

    public String getPtiUrn() {
        return ptiUrn;
    }

    public UUID getSpiResultId() {
        return spiResultId;
    }

    public UUID getMessageId() {
        return messageId;
    }
}
