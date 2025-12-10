package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.buildCorporateDefendant;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.buildCorporateDefendantWithoutContactNumber;

import uk.gov.dca.xmlschemas.libra.CourtCorporateDefendantStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.OrganisationDetails;

import org.junit.jupiter.api.Test;

public class CourtCorporateDefendantTest {

    private static final String BAIL_STATUS = "DEFAULT_VALUE";
    private String pncId = "pncId";

    @Test
    public void testBuildCourtCorporateDefendantStructure() {

        OrganisationDetails organisationDetails = buildCorporateDefendant();
        CourtCorporateDefendantStructure defendantStructure = new CourtCorporateDefendant().buildCourtCorporateDefendantStructure(organisationDetails, BAIL_STATUS, pncId);

        assertThat(defendantStructure.getPresentAtHearing(), is(organisationDetails.getPresentAtHearing()));
        assertThat(defendantStructure.getPNCidentifier(), is(pncId));
        assertThat(defendantStructure.getBailStatus(), is(BAIL_STATUS));
        assertThat(defendantStructure.getEmailDetails().getEmailAddress1(), is(organisationDetails.getContact().getPrimaryEmail()));
        assertThat(defendantStructure.getEmailDetails().getEmailAddress2(), is(organisationDetails.getContact().getSecondaryEmail()));
        assertThat(defendantStructure.getOrganisationName().getOrganisationName(), is(organisationDetails.getName()));
        assertThat(defendantStructure.getTelephoneNumberBusiness(), is(organisationDetails.getContact().getWork()));
    }

    @Test
    public void testBuildCourtCorporateDefendantStructureWithoutContactNumber() {

        OrganisationDetails organisationDetails = buildCorporateDefendantWithoutContactNumber();
        CourtCorporateDefendantStructure defendantStructure = new CourtCorporateDefendant().buildCourtCorporateDefendantStructure(organisationDetails, BAIL_STATUS, pncId);

        assertThat(defendantStructure.getEmailDetails(), nullValue());
        assertThat(defendantStructure.getTelephoneNumberBusiness(), nullValue());
        assertThat(defendantStructure.getPresentAtHearing(), is(organisationDetails.getPresentAtHearing()));
        assertThat(defendantStructure.getPNCidentifier(), is(pncId));
        assertThat(defendantStructure.getBailStatus(), is(BAIL_STATUS));
        assertThat(defendantStructure.getOrganisationName().getOrganisationName(), is(organisationDetails.getName()));

    }
}
