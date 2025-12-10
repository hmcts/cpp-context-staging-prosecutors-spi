package uk.gov.moj.cpp.staging.prosecutors.spi.events;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SyncResponse;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("stagingprosecutorsspi.event.unsuccessful-sync-response-recorded")
public class UnsuccessfulSyncResponseRecorded {


    private final SyncResponse syncResponse;

    @JsonCreator
    public UnsuccessfulSyncResponseRecorded(final SyncResponse syncResponse) {
        this.syncResponse = syncResponse;
    }

    public SyncResponse getSyncResponse() {
        return syncResponse;
    }
}
