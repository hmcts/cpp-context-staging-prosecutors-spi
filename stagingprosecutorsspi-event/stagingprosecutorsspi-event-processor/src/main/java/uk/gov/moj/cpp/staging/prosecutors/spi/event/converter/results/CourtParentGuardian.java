package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.buildBasePersonDetailStructure;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.buildCourtAddressStructure;

import uk.gov.dca.xmlschemas.libra.CourtParentGuardianStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.AssociatedIndividual;

public class CourtParentGuardian {

    public CourtParentGuardianStructure buildParentGuardian(final AssociatedIndividual associatedPerson) {
        final CourtParentGuardianStructure courtParentGuardianStructure = new CourtParentGuardianStructure();
        courtParentGuardianStructure.setAddress(buildCourtAddressStructure(associatedPerson.getPerson().getAddress()));
        courtParentGuardianStructure.setBasePersonDetails(buildBasePersonDetailStructure(associatedPerson.getPerson()));
        return courtParentGuardianStructure;
    }
}
