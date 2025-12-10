package uk.gov.moj.cpp.staging.soap.schema.converter;


import uk.gov.dca.xmlschemas.libra.PoliceVictimStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceVictim;

import java.util.ArrayList;
import java.util.List;

public class PoliceVictimsConverter implements Converter<List<PoliceVictimStructure>, List<PoliceVictim>> {

    @Override
    public List<PoliceVictim> convert(final List<PoliceVictimStructure> policeVictimStructures) {
        final List<PoliceVictim> policeVictims = new ArrayList<>();

        if (null != policeVictimStructures) {
            final PoliceVictimConverter policeVictimConverter = new PoliceVictimConverter();

            for (final PoliceVictimStructure policeVictimStructure : policeVictimStructures) {
                final PoliceVictim policeVictim = policeVictimConverter.convert(policeVictimStructure);

                policeVictims.add(policeVictim);
            }
        }

        return policeVictims;
    }

}