package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonDetail;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonName;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceIndividualDefendant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PersonalInformationConverterTest {
    @Mock
    private AddressConverter addressConverter;

    @Mock
    private ContactDetailsConverter contactDetailsConverter;

    @Mock
    private PoliceIndividualDefendant policeIndividualDefendant;

    @Mock
    private BasePersonDefendant basePersonDefendant;

    @Mock
    private BasePersonDetail basePersonDetail;

    @Mock
    private BasePersonName basePersonName;

    @Mock
    private Address address;

    @Mock
    private ContactDetails contactDetails;

    @InjectMocks
    private PersonalInformationConverter personalInformationConverter;

    private static final String TITLE = "Mr";
    private static final String GIVEN_NAME = "John";
    private static final String FAMILY_NAME = "Smith";
    private static final uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address defendantAddress = uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address.address()
            .withPostcode("AA1 2BB")
            .withPaon("          AAAA BBBBB")
            .build();

    @Test
    public void convert() {
        when(policeIndividualDefendant.getPersonDefendant()).thenReturn(basePersonDefendant);
        when(policeIndividualDefendant.getAddress()).thenReturn(defendantAddress);
        when(basePersonDefendant.getBasePersonDetails()).thenReturn(basePersonDetail);
        when(basePersonDetail.getPersonName()).thenReturn(basePersonName);
        when(basePersonName.getPersonGivenName1()).thenReturn(GIVEN_NAME);
        when(basePersonName.getPersonFamilyName()).thenReturn(FAMILY_NAME);
        when(basePersonName.getPersonTitle()).thenReturn(TITLE);

        PersonalInformation personalInformation = personalInformationConverter.convert(policeIndividualDefendant);
        assertNotNull(personalInformation);
        assertThat(personalInformation.getFirstName(), is(GIVEN_NAME));
        assertThat(personalInformation.getLastName(), is(FAMILY_NAME));
        assertThat(personalInformation.getTitle(), is(TITLE));
        assertThat(personalInformation.getAddress(), isA(Address.class));
        assertThat(personalInformation.getContactDetails(), isA(ContactDetails.class));
    }
}
