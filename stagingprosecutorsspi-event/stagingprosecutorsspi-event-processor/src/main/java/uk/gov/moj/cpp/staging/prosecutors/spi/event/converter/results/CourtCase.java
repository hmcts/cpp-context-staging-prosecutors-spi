package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import uk.gov.dca.xmlschemas.libra.CourtCaseStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.PublicPoliceResultGenerated;

import java.util.Map;

import javax.inject.Inject;
import javax.xml.datatype.XMLGregorianCalendar;

public class CourtCase {

    @Inject
    private CourtDefendant courtDefendant;

    public CourtCaseStructure buildCourtCaseStructure(final PublicPoliceResultGenerated source, final Integer psaCode, final Map<String, Object> context, final XMLGregorianCalendar dateOfHearing) {
        final CourtCaseStructure courtCaseStructure = new CourtCaseStructure();
        courtCaseStructure.setPTIURN(source.getUrn());
        courtCaseStructure.setDefendant(courtDefendant.buildCourtDefendantStructure(source.getDefendant(), psaCode, context, dateOfHearing));
        return courtCaseStructure;
    }

}
