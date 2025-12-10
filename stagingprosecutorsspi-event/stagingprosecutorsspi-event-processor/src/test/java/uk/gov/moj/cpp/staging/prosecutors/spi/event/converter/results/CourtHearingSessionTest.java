package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static java.math.BigInteger.valueOf;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.buildCourtCentreWithLJA;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.buildListOfSessionDay;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.getXmlGregorianCalendar;

import uk.gov.dca.xmlschemas.libra.CourtHearingStructure;

import org.junit.jupiter.api.Test;

public class CourtHearingSessionTest {

    @Test
    public void testBuildCourtHearingSession() {

        CourtHearingStructure courtHearingStructure = new CourtHearingSession().buildCourtHearingSession(buildListOfSessionDay(),buildCourtCentreWithLJA());
        assertNotNull(courtHearingStructure);
        assertThat(courtHearingStructure.getPSAcode(), is(valueOf(buildCourtCentreWithLJA().getPsaCode())));
        assertHearing(courtHearingStructure);
    }

    private void assertHearing(final CourtHearingStructure courtHearingStructure) {
        if(null != courtHearingStructure.getHearing()) {
            assertThat(courtHearingStructure.getHearing().getCourtHearingLocation(), is(buildCourtCentreWithLJA().getCourtHearingLocation()));
            assertThat(courtHearingStructure.getHearing().getDateOfHearing(), is(getXmlGregorianCalendar(buildListOfSessionDay().get(0).getSittingDay())));
            assertThat(courtHearingStructure.getHearing().getTimeOfHearing(), is(getXmlGregorianCalendar(buildListOfSessionDay().get(0).getSittingDay())));
        }
    }
}
