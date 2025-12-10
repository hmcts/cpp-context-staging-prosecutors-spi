package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class IndividualDefendantConverterTest {

    @Mock
    private PersonalInformationConverter personalInformationConverter;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PoliceIndividualDefendant policeIndividualDefendant;

    @Mock
    private PersonalInformation personalInformation;

    @Mock
    private SelfDefinedInformation selfDefinedInformation;

    @Mock
    private SelfDefinedInformationConverter selfDefinedInformationConverter;

    @InjectMocks
    private IndividualDefendantConverter individualDefendantConverter;

    private static final String DRIVER_NUMBER = "driverNumber";
    private static final String OFFENDER_CODE = "offenderCode";
    private static final String PERCEIVEDBIRTHYEAR = "perceivedBirthYear";

    private static final BasePersonName PERSON_NAME = BasePersonName.basePersonName()
            .withPersonFamilyName("Smith")
            .withPersonGivenName1("John")
            .withPersonTitle("Mr")
            .build();
    private static final Address address = Address.address()
            .withPostcode("AA1 2BB")
            .withSaon("          DDDD CCCCC")
            .withPaon("          AAAA BBBBB")
            .build();

    private static final PoliceParentGuardian policeParentGuardian =
            PoliceParentGuardian.policeParentGuardian()
            .withAddress(address)
            .withBasePersonDetails(BasePersonDetail.basePersonDetail().withPersonName(PERSON_NAME).build()).
                    build();

    @Test
    public void convert() {
        when(policeIndividualDefendant.getDriverNumber()).thenReturn(DRIVER_NUMBER);
        when(policeIndividualDefendant.getPersonDefendant().getBasePersonDetails().getPersonName()).thenReturn(PERSON_NAME);
        when(policeIndividualDefendant.getAddress()).thenReturn(address);
        when(policeIndividualDefendant.getOffenderCode()).thenReturn(OFFENDER_CODE);
        when(policeIndividualDefendant.getPerceivedBirthYear()).thenReturn(PERCEIVEDBIRTHYEAR);
        when(policeIndividualDefendant.getParentGuardianDetails()).thenReturn(policeParentGuardian);

        Individual individual = individualDefendantConverter.convert(policeIndividualDefendant);
        assertNotNull(individual);
        assertThat(individual.getDriverNumber(), is(DRIVER_NUMBER));
        assertThat(individual.getPersonalInformation(), isA(PersonalInformation.class));
        assertThat(individual.getSelfDefinedInformation(), isA(SelfDefinedInformation.class));
        assertThat(individual.getOffenderCode(), is(OFFENDER_CODE));
        assertThat(individual.getPerceivedBirthYear(), is(PERCEIVEDBIRTHYEAR));
    }
}

