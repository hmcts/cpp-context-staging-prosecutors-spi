package uk.gov.moj.cpp.staging.prosecutors.spi.events;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("stagingprosecutorsspi.event.spi-oi-police-system-updated")
public class OiPoliceSystemUpdated {

    private final UUID oiId;

    private final  String policeSystemId;

    public OiPoliceSystemUpdated(UUID oiId, String policeSystemId) {
        this.oiId = oiId;
        this.policeSystemId = policeSystemId;
    }

    public UUID getOiId() {
        return oiId;
    }

    public String getPoliceSystemId() {
        return policeSystemId;
    }
}
