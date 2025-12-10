package uk.gov.moj.cpp.staging.soap.schema.converter;

import uk.gov.dca.xmlschemas.libra.PoliceCaseStructure;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiInitialHearing;

public class HearingConverter implements Converter<PoliceCaseStructure, SpiInitialHearing> {

    @Override
    public SpiInitialHearing convert(final PoliceCaseStructure source) {
        return new SpiInitialHearing.Builder().withCourtHearingLocation(source.getInitialHearing().getCourtHearingLocation())
                .withDateOfHearing(source.getInitialHearing().getDateOfHearing().toGregorianCalendar().toZonedDateTime().toLocalDate())
                .withTimeOfHearing(new TimeConverter().convert(source.getInitialHearing().getTimeOfHearing()))
                .build();
    }

}
