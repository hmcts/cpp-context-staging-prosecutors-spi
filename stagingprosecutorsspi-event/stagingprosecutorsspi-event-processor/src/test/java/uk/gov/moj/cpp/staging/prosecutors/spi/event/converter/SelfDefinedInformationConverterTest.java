package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonDetail;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceIndividualDefendant;

import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SelfDefinedInformationConverterTest {

    @Mock
    private PoliceIndividualDefendant policeIndividualDefendant;

    @Mock
    private BasePersonDefendant basePersonDefendant;

    @Mock
    private BasePersonDetail basePersonDetail;

    @InjectMocks
    private SelfDefinedInformationConverter selfDefinedInformationConverter;

    private static final Integer GENDER_TYPE_MALE = 1;
    private static final Integer GENDER_TYPE_FEMALE = 2;
    private static final Integer GENDER_TYPE_NOT_KNOWN = 0;
    private static final Integer GENDER_TYPE_NOT_SPECIFIED = 9;
    private static final LocalDate BIRTHDATE = ZonedDateTime.now().toLocalDate();
    private static final String NATIONALITY = "NATIONALITY";

    @Test
    public void convertWhenGenderTypeIsMale() {
        when(policeIndividualDefendant.getPersonDefendant()).thenReturn(basePersonDefendant);
        when(basePersonDefendant.getBasePersonDetails()).thenReturn(basePersonDetail);
        when(basePersonDetail.getGender()).thenReturn(GENDER_TYPE_MALE);
        when((basePersonDetail.getBirthDate())).thenReturn(BIRTHDATE);

        SelfDefinedInformation selfDefinedInformation = selfDefinedInformationConverter.convert(policeIndividualDefendant);
        assertNotNull(selfDefinedInformation);
        assertThat(selfDefinedInformation.getGender(), is(Gender.MALE));
        assertThat(selfDefinedInformation.getNationality(), is(nullValue()));
        assertThat(selfDefinedInformation.getDateOfBirth(), is(BIRTHDATE));
    }

    @Test
    public void convertWhenGenderTypeIsFemale() {
        when(policeIndividualDefendant.getPersonDefendant()).thenReturn(basePersonDefendant);
        when(basePersonDefendant.getBasePersonDetails()).thenReturn(basePersonDetail);
        when(basePersonDetail.getGender()).thenReturn(GENDER_TYPE_FEMALE);
        when((basePersonDetail.getBirthDate())).thenReturn(BIRTHDATE);

        SelfDefinedInformation selfDefinedInformation = selfDefinedInformationConverter.convert(policeIndividualDefendant);
        assertNotNull(selfDefinedInformation);
        assertThat(selfDefinedInformation.getGender(), is(Gender.FEMALE));
        assertThat(selfDefinedInformation.getNationality(), is(nullValue()));
        assertThat(selfDefinedInformation.getDateOfBirth(), is(BIRTHDATE));
    }

    @Test
    public void convertWhenGenderTypeIsNotKnown() {
        when(policeIndividualDefendant.getPersonDefendant()).thenReturn(basePersonDefendant);
        when(basePersonDefendant.getBasePersonDetails()).thenReturn(basePersonDetail);
        when(basePersonDetail.getGender()).thenReturn(GENDER_TYPE_NOT_KNOWN);
        when((basePersonDetail.getBirthDate())).thenReturn(BIRTHDATE);

        SelfDefinedInformation selfDefinedInformation = selfDefinedInformationConverter.convert(policeIndividualDefendant);
        assertNotNull(selfDefinedInformation);
        assertThat(selfDefinedInformation.getGender(), is(Gender.NOT_KNOWN));
        assertThat(selfDefinedInformation.getNationality(), is(nullValue()));
        assertThat(selfDefinedInformation.getDateOfBirth(), is(BIRTHDATE));
    }

    @Test
    public void convertWhenGenderTypeIsNotSpecified() {
        when(policeIndividualDefendant.getPersonDefendant()).thenReturn(basePersonDefendant);
        when(basePersonDefendant.getBasePersonDetails()).thenReturn(basePersonDetail);
        when(basePersonDetail.getGender()).thenReturn(GENDER_TYPE_NOT_SPECIFIED);
        when((basePersonDetail.getBirthDate())).thenReturn(BIRTHDATE);

        SelfDefinedInformation selfDefinedInformation = selfDefinedInformationConverter.convert(policeIndividualDefendant);
        assertNotNull(selfDefinedInformation);
        assertThat(selfDefinedInformation.getGender(), is(Gender.NOT_SPECIFIED));
        assertThat(selfDefinedInformation.getNationality(), is(nullValue()));
        assertThat(selfDefinedInformation.getDateOfBirth(), is(BIRTHDATE));
    }

    @Test
    public void convertWhenGenderTypeIsEmptyOrNull() {
        when(policeIndividualDefendant.getPersonDefendant()).thenReturn(basePersonDefendant);
        when(basePersonDefendant.getBasePersonDetails()).thenReturn(basePersonDetail);
        when(basePersonDetail.getGender()).thenReturn(null);
        when((basePersonDetail.getBirthDate())).thenReturn(BIRTHDATE);

        SelfDefinedInformation selfDefinedInformation = selfDefinedInformationConverter.convert(policeIndividualDefendant);
        assertNotNull(selfDefinedInformation);
        assertThat(selfDefinedInformation.getGender(), is(nullValue()));
        assertThat(selfDefinedInformation.getNationality(), is(nullValue()));
        assertThat(selfDefinedInformation.getDateOfBirth(), is(BIRTHDATE));
    }

}