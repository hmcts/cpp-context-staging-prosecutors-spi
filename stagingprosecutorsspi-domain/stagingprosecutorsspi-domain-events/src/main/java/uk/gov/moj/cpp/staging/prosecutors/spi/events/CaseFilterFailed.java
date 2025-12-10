package uk.gov.moj.cpp.staging.prosecutors.spi.events;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event("stagingprosecutorsspi.event.case-filter-failed")
public class CaseFilterFailed {

    private final UUID caseId;

    @JsonCreator
    public CaseFilterFailed(final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
