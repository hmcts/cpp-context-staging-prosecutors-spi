package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import uk.gov.dca.xmlschemas.libra.CourtSessionStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.PublicPoliceResultGenerated;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CourtCentreWithLJA;

import java.util.Map;

import javax.inject.Inject;
import javax.xml.datatype.XMLGregorianCalendar;

public class CourtSession {

    @Inject
    private CourtCase courtCase;

    @Inject
    private CourtHearingSession courtHearingSession;

    public CourtSessionStructure buildCourtSession(final PublicPoliceResultGenerated source, final Map<String, Object> context) {
        final CourtSessionStructure courtSessionStructure = new CourtSessionStructure();
        final CourtCentreWithLJA courtCentreWithLJA = source.getCourtCentreWithLJA();
        courtSessionStructure.setCourtHearing(courtHearingSession.buildCourtHearingSession(source.getSessionDays(), courtCentreWithLJA));
        final XMLGregorianCalendar dateOfHearing = courtSessionStructure.getCourtHearing().getHearing().getDateOfHearing();
        courtSessionStructure.getCase().add(courtCase.buildCourtCaseStructure(source, courtCentreWithLJA.getPsaCode(), context, dateOfHearing));
        return courtSessionStructure;
    }
}
