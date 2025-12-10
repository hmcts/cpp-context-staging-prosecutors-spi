package uk.gov.moj.cpp.staging.prosecutors.spi.events;

import uk.gov.cjse.schemas.endpoint.types.SubmitRequest;
import uk.gov.justice.domain.annotation.Event;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("stagingprosecutorsspi.event.cppmessage-prepared-for-sending")
public class CPPMessagePreparedForSending {


    private final SubmitRequest submitRequest;

    @JsonCreator
    public CPPMessagePreparedForSending(final SubmitRequest submitRequest) {
        this.submitRequest = submitRequest;
    }

    public SubmitRequest getSubmitRequest() {
        return submitRequest;
    }
}
