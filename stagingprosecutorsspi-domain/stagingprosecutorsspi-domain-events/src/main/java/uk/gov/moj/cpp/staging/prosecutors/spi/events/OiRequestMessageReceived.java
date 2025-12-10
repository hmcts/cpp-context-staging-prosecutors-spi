package uk.gov.moj.cpp.staging.prosecutors.spi.events;

import uk.gov.cjse.schemas.common.operations.RouteDataRequestType;
import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("stagingprosecutorsspi.event.spi-oi-request-received")
public class OiRequestMessageReceived {
    private final String correlationId;

    private final String dataStreamContent;

    private final UUID oiId;

    private final String systemId;

    private final String policeSystemId;

    private final RouteDataRequestType routeDataRequestType;

    @JsonCreator
    public OiRequestMessageReceived(final String correlationId, final String dataStreamContent, final UUID oiId, final String systemId, final RouteDataRequestType routeDataRequestType, final String policeSystemId) {
        this.correlationId = correlationId;
        this.dataStreamContent = dataStreamContent;
        this.oiId = oiId;
        this.systemId = systemId;
        this.policeSystemId = policeSystemId;
        this.routeDataRequestType = routeDataRequestType;
    }

    public static Builder oiRequestMessageReceived() {
        return new OiRequestMessageReceived.Builder();
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getDataStreamContent() {
        return dataStreamContent;
    }

    public UUID getOiId() {
        return oiId;
    }

    public String getSystemId() {
        return systemId;
    }

    public String getPoliceSystemId() {
        return policeSystemId;
    }

    public RouteDataRequestType getRouteDataRequestType() {
        return routeDataRequestType;
    }

    public static class Builder {
        private String correlationId;

        private String dataStreamContent;

        private UUID oiId;

        private String systemId;

        private String policeSystemId;

        private RouteDataRequestType routeDataRequestType;

        public Builder withCorrelationId(final String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder withDataStreamContent(final String dataStreamContent) {
            this.dataStreamContent = dataStreamContent;
            return this;
        }

        public Builder withOiId(final UUID oiId) {
            this.oiId = oiId;
            return this;
        }

        public Builder withSystemId(final String systemId) {
            this.systemId = systemId;
            return this;
        }

        public Builder withRouteDataRequestType(final RouteDataRequestType routeDataRequestType) {
            this.routeDataRequestType = routeDataRequestType;
            return this;
        }

        public Builder withPoliceSystemId(final String policeSystemId) {
            this.policeSystemId = policeSystemId;
            return this;
        }

        public OiRequestMessageReceived build() {
            return new OiRequestMessageReceived(correlationId, dataStreamContent, oiId, systemId, routeDataRequestType, policeSystemId);
        }
    }
}
