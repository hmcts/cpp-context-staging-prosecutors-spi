package uk.gov.moj.cpp.staging.soap.schema.converter;

import static java.util.UUID.randomUUID;

import uk.gov.dca.xmlschemas.libra.PoliceCaseStructure;
import uk.gov.dca.xmlschemas.libra.PoliceDefendantStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceDefendant;

import java.util.ArrayList;
import java.util.List;

public class PoliceDefendantConverter implements Converter<PoliceCaseStructure, List<PoliceDefendant>> {

    @Override
    public List<PoliceDefendant> convert(final PoliceCaseStructure source) {

        final List<PoliceDefendant> policeDefendants = new ArrayList<>();

        for (final PoliceDefendantStructure defendant : source.getDefendant()) {
            final PoliceDefendant policeDefendant = new PoliceDefendant.Builder().
                    withId(randomUUID()).
                    withProsecutorReference(defendant.getProsecutorReference()).
                    withPoliceCorporateDefendant(defendant.getPoliceCorporateDefendant() == null ? null : new PoliceCorporateDefendantConverter().convert(defendant.getPoliceCorporateDefendant())).
                    withPoliceIndividualDefendant(defendant.getPoliceIndividualDefendant() == null ? null : new PoliceIndividualDefendantConverter().convert(defendant.getPoliceIndividualDefendant())).
                    withOffence(new PoliceOffenceConverter().convert(defendant.getOffence())).
                    build();
            policeDefendants.add(policeDefendant);
        }
        return policeDefendants;

    }

}


