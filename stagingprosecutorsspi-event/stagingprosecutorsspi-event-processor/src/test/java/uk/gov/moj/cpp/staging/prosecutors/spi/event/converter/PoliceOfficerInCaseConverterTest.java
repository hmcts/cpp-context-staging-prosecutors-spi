package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonName;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceOfficerInCase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PoliceOfficerInCaseConverterTest {

    @InjectMocks
    private PoliceOfficerInCaseConverter policeOfficerInCaseConverter;

    @Mock
    private PoliceOfficerInCase policeOfficerInCase;

    private static final String POLICE_RANK = "Constable";
    private static final String TITLE = "Mr";
    private static final String FIRST_NAME = "Donny";
    private static final String LAST_NAME = "Van de Beek";
    private static final String HOME_NUMBER = "01113 212121";
    private static final String WORK_NUMBER = "01114 323232";
    private static final String MOBILE_NUMBER = "07121 454545";
    private static final String PRIMARY_EMAIL = "donnyvandebeek@gmail.com";
    private static final String SECONDARY_EMAIL = "dvandebeek@hotmail.co.uk";

    @Test
    public void convertPoliceOfficerInCase() {

        final PoliceOfficerInCase policeOfficerInCase = PoliceOfficerInCase.policeOfficerInCase()
                .withStructuredAddress(getAddress())
                .withTelephoneNumberHome(HOME_NUMBER)
                .withTelephoneNumberMobile(MOBILE_NUMBER)
                .withTelephoneNumberBusiness(WORK_NUMBER)
                .withEmailAddress1(PRIMARY_EMAIL)
                .withEmailAddress2(SECONDARY_EMAIL)
                .withPoliceOfficerRank(POLICE_RANK)
                .withOfficerName(BasePersonName.basePersonName()
                        .withPersonFamilyName(LAST_NAME)
                        .withPersonGivenName1(FIRST_NAME)
                        .withPersonTitle(TITLE)
                        .build())
                .build();
        uk.gov.moj.cpp.prosecution.casefile.json.schemas.PoliceOfficerInCase pcfPoliceOfficerInCase =
                policeOfficerInCaseConverter.convert(policeOfficerInCase);

        assertNotNull(pcfPoliceOfficerInCase);
        assertThat(pcfPoliceOfficerInCase.getPoliceOfficerRank(), is(POLICE_RANK));
        assertNotNull(pcfPoliceOfficerInCase.getPersonalInformation());
        assertThat(pcfPoliceOfficerInCase.getPersonalInformation().getFirstName(), is(FIRST_NAME));
        assertThat(pcfPoliceOfficerInCase.getPersonalInformation().getLastName(), is(LAST_NAME));
        assertThat(pcfPoliceOfficerInCase.getPersonalInformation().getTitle(), is(TITLE));
        assertNotNull(pcfPoliceOfficerInCase.getPersonalInformation().getContactDetails());
        assertThat(pcfPoliceOfficerInCase.getPersonalInformation().getContactDetails().getHome(), is(HOME_NUMBER));
        assertThat(pcfPoliceOfficerInCase.getPersonalInformation().getContactDetails().getMobile(), is(MOBILE_NUMBER));
        assertThat(pcfPoliceOfficerInCase.getPersonalInformation().getContactDetails().getWork(), is(WORK_NUMBER));
        assertThat(pcfPoliceOfficerInCase.getPersonalInformation().getContactDetails().getPrimaryEmail(), is(PRIMARY_EMAIL));
        assertThat(pcfPoliceOfficerInCase.getPersonalInformation().getContactDetails().getSecondaryEmail(), is(SECONDARY_EMAIL));
    }

    private Address getAddress() {
        return Address.address()
                .withPaon("682 Essex Electrical Supplies")
                .withStreetDescription("Green Lane")
                .withTown("Ilford")
                .withAdministrativeArea("Essex")
                .withPostcode("IG3 9RX")
                .build();
    }

}
