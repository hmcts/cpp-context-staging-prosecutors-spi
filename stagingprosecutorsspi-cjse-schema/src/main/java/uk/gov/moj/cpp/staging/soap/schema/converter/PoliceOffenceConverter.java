package uk.gov.moj.cpp.staging.soap.schema.converter;


import uk.gov.dca.xmlschemas.libra.PoliceOffenceStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceOffense;

import java.util.ArrayList;
import java.util.List;

public class PoliceOffenceConverter implements Converter<List<PoliceOffenceStructure>, List<PoliceOffense>> {

    @Override
    public List<PoliceOffense> convert(final List<PoliceOffenceStructure> policeOffenceStructures) {
        final List<PoliceOffense> policeOffenses = new ArrayList<>();
        for (final PoliceOffenceStructure policeDefendantStructure : policeOffenceStructures) {
            final PoliceOffense policeOffense = new PoliceOffense.Builder().
                    withBaseOffenceDetails(new BaseOffenseConverter().convert(policeDefendantStructure.getBaseOffenceDetails())).
                    withOtherPartyVictim(new PoliceVictimsConverter().convert(policeDefendantStructure.getOtherPartyVictim())).
                    withProsecutionFacts(policeDefendantStructure.getProsecutionFacts()).build();
            policeOffenses.add(policeOffense);
        }

        return policeOffenses;
    }

}
