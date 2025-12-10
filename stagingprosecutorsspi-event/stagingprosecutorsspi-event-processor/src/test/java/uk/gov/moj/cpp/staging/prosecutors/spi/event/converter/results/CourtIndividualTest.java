package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.buildCaseDefendant;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.buildCaseDefendantNoBailConditions;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.buildCaseDefendantV1;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.buildCaseDefendantV2;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.buildCaseDefendantV3;

import uk.gov.dca.xmlschemas.libra.CourtIndividualDefendantStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.IndividualDefendant;

import org.junit.jupiter.api.Test;

public class CourtIndividualTest {

    @Test
    public void testBuildCourtIndividualDefendantStructure() {

        CourtIndividualDefendantStructure defendantStructure = new CourtIndividual().buildCourtIndividualDefendantStructure(buildCaseDefendant());

        IndividualDefendant individualDefendant = buildCaseDefendant().getIndividualDefendant();
        String pncId = buildCaseDefendant().getPncId();

        assertThat(defendantStructure.getPresentAtHearing(), is(individualDefendant.getPresentAtHearing()));
        assertThat(defendantStructure.getBailStatus(), is(individualDefendant.getBailStatus().getCode()));
        assertThat(defendantStructure.getPresentAtHearing(), is(individualDefendant.getPresentAtHearing()));
        assertThat(defendantStructure.getReasonForBailConditionsOrCustody(), is(individualDefendant.getReasonForBailConditionsOrCustody()));
        assertThat(defendantStructure.getBailStatus(), is(individualDefendant.getBailStatus().getCode()));
        assertPersonDefendant(defendantStructure, individualDefendant, pncId);

    }

    @Test
    public void testBuildCourtIndividualDefendantStructureWithBailConditionsLimit() {

        CourtIndividualDefendantStructure defendantStructure = new CourtIndividual().buildCourtIndividualDefendantStructure(buildCaseDefendantV1());

        assertThat(defendantStructure.getPersonDefendant().getBailConditions(), notNullValue());
        assertEquals(2500, defendantStructure.getPersonDefendant().getBailConditions().length());
        assertNotEquals(";", defendantStructure.getPersonDefendant().getBailConditions().endsWith(";"));
    }

    @Test
    public void testBuildCourtIndividualDefendantStructureWithBailConditionsWithoutSemiColon1() {

        CourtIndividualDefendantStructure defendantStructure = new CourtIndividual().buildCourtIndividualDefendantStructure(buildCaseDefendantV2());

        assertThat(defendantStructure.getPersonDefendant().getBailConditions(), notNullValue());
        assertNotEquals(";", defendantStructure.getPersonDefendant().getBailConditions().endsWith(";"));
    }

    @Test
    public void testBuildCourtIndividualDefendantStructureWithBailConditionsWithoutSemiColon2() {

        CourtIndividualDefendantStructure defendantStructure = new CourtIndividual().buildCourtIndividualDefendantStructure(buildCaseDefendantV3());
        assertThat(defendantStructure.getPersonDefendant().getBailConditions(), notNullValue());
        assertNotEquals(";", defendantStructure.getPersonDefendant().getBailConditions().endsWith(";"));
    }

    @Test
    public void testBuildCourtIndividualDefendantStructureWithNoBailConditions() {

        CourtIndividualDefendantStructure defendantStructure = new CourtIndividual().buildCourtIndividualDefendantStructure(buildCaseDefendantNoBailConditions());
        assertThat(defendantStructure.getPersonDefendant().getBailConditions(), nullValue());
    }

    private void assertPersonDefendant(final CourtIndividualDefendantStructure defendantStructure, final IndividualDefendant individualDefendant, final String pncId) {
        assertThat(defendantStructure.getPersonDefendant().getBailConditions(), is(individualDefendant.getBailConditions()));
        assertThat(defendantStructure.getPersonDefendant().getPersonStatedNationality(), is(nullValue()));
        assertThat(defendantStructure.getPersonDefendant().getPNCidentifier(), is(pncId));
    }

}
