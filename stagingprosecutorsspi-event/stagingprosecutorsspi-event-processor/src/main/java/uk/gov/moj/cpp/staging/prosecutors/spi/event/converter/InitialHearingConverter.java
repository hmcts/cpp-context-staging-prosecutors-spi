package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import static uk.gov.moj.cpp.staging.prosecutors.spi.event.utils.SpiUtil.ifNotNull;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiInitialHearing;

public class InitialHearingConverter implements Converter<SpiInitialHearing, InitialHearing> {

    @Override
    public InitialHearing convert(final SpiInitialHearing source) {
        return ifNotNull(source, InitialHearing.initialHearing()
                .withCourtHearingLocation(source.getCourtHearingLocation())
                .withDateOfHearing(source.getDateOfHearing().toString())
                .withTimeOfHearing(source.getTimeOfHearing())
                .build());
    }
}

