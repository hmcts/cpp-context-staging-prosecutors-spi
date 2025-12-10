package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import static java.util.stream.Collectors.toList;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiInitialHearing;

import java.util.List;

public class CcDefendantConverter implements SpiConverter<List<PoliceDefendant>, List<Defendant>, SpiInitialHearing> {

    public List<Defendant> convert(final List<PoliceDefendant> policeDefendantList, final SpiInitialHearing initialHearing) {

        return policeDefendantList.stream()
                .map(policeDefendant -> new DefendantConverter().convert(policeDefendant, initialHearing))
                .collect(toList());
    }
}