package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static java.math.BigInteger.valueOf;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.getXmlGregorianCalendar;

import uk.gov.dca.xmlschemas.libra.BaseHearingStructure;
import uk.gov.dca.xmlschemas.libra.CourtHearingStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CourtCentreWithLJA;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SessionDay;

import java.util.List;
import java.util.Optional;

public class CourtHearingSession {

    public CourtHearingStructure buildCourtHearingSession(final List<SessionDay> sessionDays, final CourtCentreWithLJA courtCentreWithLJA) {
        final CourtHearingStructure courtHearingStructure = new CourtHearingStructure();
        final BaseHearingStructure baseHearingStructure = new BaseHearingStructure();

        baseHearingStructure.setCourtHearingLocation(courtCentreWithLJA.getCourtHearingLocation());
        final Optional<SessionDay> first = sessionDays.stream().findFirst();
        if (first.isPresent()) {
            final SessionDay sessionDay = first.get();
            baseHearingStructure.setDateOfHearing(getXmlGregorianCalendar(sessionDay.getSittingDay()));
            baseHearingStructure.setTimeOfHearing(getXmlGregorianCalendar(sessionDay.getSittingDay()));
        }
        courtHearingStructure.setHearing(baseHearingStructure);
        courtHearingStructure.setPSAcode(valueOf(courtCentreWithLJA.getPsaCode()));
        return courtHearingStructure;
    }


}
