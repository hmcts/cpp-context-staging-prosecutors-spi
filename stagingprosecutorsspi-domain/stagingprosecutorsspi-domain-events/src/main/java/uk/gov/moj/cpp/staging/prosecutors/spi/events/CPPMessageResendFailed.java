package uk.gov.moj.cpp.staging.prosecutors.spi.events;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("stagingprosecutorsspi.event.cppmessage-resend-failed")
public class CPPMessageResendFailed {


    private final UUID id;

    @JsonCreator
    public CPPMessageResendFailed(final UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
