package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.buildAssociatedIndividual;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.getXmlGregorianCalendar;

import uk.gov.dca.xmlschemas.libra.CourtParentGuardianStructure;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Individual;

import org.junit.jupiter.api.Test;

public class CourtParentGuardianTest {

    @Test
    public void testBuildParentGuardian() {

        CourtParentGuardianStructure courtParentGuardianStructure = new CourtParentGuardian().buildParentGuardian(buildAssociatedIndividual());
        Individual individual =  buildAssociatedIndividual().getPerson();

        assertThat(courtParentGuardianStructure.getBasePersonDetails().getBirthdate(), is(getXmlGregorianCalendar(individual.getDateOfBirth())));
        assertEmailDetails(courtParentGuardianStructure, individual);
        assertTelephoneDetails(courtParentGuardianStructure, individual);
        assertPersonName(courtParentGuardianStructure, individual);


    }

    private void assertPersonName(final CourtParentGuardianStructure courtParentGuardianStructure, final Individual individual) {
        if(null != courtParentGuardianStructure.getBasePersonDetails().getPersonName()){
        assertThat(courtParentGuardianStructure.getBasePersonDetails().getPersonName().getPersonFamilyName(), is(individual.getLastName()));
        assertThat(courtParentGuardianStructure.getBasePersonDetails().getPersonName().getPersonGivenName1(), is(individual.getFirstName()));
        assertThat(courtParentGuardianStructure.getBasePersonDetails().getPersonName().getPersonGivenName2(), is(individual.getMiddleName()));
        assertThat(courtParentGuardianStructure.getBasePersonDetails().getPersonName().getPersonTitle(), is(individual.getTitle().toString()));
        }
    }

    private void assertTelephoneDetails(final CourtParentGuardianStructure courtParentGuardianStructure, final Individual individual) {
        if(null != courtParentGuardianStructure.getBasePersonDetails().getTelephoneDetails()) {
            assertThat(courtParentGuardianStructure.getBasePersonDetails().getTelephoneDetails().getTelephoneNumberHome(), is(individual.getContact().getHome()));
            assertThat(courtParentGuardianStructure.getBasePersonDetails().getTelephoneDetails().getTelephoneNumberMobile(), is(individual.getContact().getMobile()));
            assertThat(courtParentGuardianStructure.getBasePersonDetails().getTelephoneDetails().getTelephoneNumberBusiness(), is(individual.getContact().getWork()));
        }
    }

    private void assertEmailDetails(final CourtParentGuardianStructure courtParentGuardianStructure, final Individual individual) {
        if(null != courtParentGuardianStructure.getBasePersonDetails().getEmailDetails()){
        assertThat(courtParentGuardianStructure.getBasePersonDetails().getEmailDetails().getEmailAddress1(), is(individual.getContact().getPrimaryEmail()));
        assertThat(courtParentGuardianStructure.getBasePersonDetails().getEmailDetails().getEmailAddress2(), is (individual.getContact().getSecondaryEmail()));
        }
    }
}
