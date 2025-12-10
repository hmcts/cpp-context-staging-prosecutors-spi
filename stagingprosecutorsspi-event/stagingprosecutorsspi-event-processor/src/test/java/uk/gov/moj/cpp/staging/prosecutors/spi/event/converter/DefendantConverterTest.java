package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.IndividualAlias.individualAlias;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonName.basePersonName;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.IndividualAlias;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonDetail;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.BasePersonName;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceCorporateDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceIndividualDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.PoliceParentGuardian;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SpiInitialHearing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefendantConverterTest {

    @Mock
    private IndividualDefendantConverter individualDefendantConverter;

    @Mock
    private PoliceDefendant policeDefendant;

    @Mock
    private Individual individual;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PoliceIndividualDefendant policeIndividualDefendant;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BasePersonDefendant basePersonDefendant;

    @InjectMocks
    private DefendantConverter defendantConverter;

    @Mock
    private PoliceCorporateDefendant policeCorporateDefendant;

    private static final String HEARING_LANGUAGE = "E";
    private static final String DOCUMENTATION_LANGUAGE = "W";
    private static final String LANGUAGE_NEEDS = "INTERPRETER";
    private static final String SPECIAL_REQUIREMENTS = "WHEELCHAIR";
    private static final String PROSECUTOR_REFERENCE = "REF";
    private static final String PERSON_TITLE = "Title";
    private static final String PERSON_GIVEN_NAME_1 = "GivenName1";
    private static final String PERSON_GIVEN_NAME_2 = "GivenName2";
    private static final String PERSON_GIVEN_NAME_3 = "GivenName3";
    private static final String PERSON_FAMILY_NAME = "FamilyName";
    private static final String PNC_IDENTIFIER = "PncIdentifier";

    private static final BasePersonName PERSON_NAME = basePersonName()
            .withPersonFamilyName("Smith")
            .withPersonGivenName1("John")
            .withPersonTitle("Mr")
            .build();
    private static final Address address = Address.address()
            .withPostcode("AA1 2BB")
            .withPaon("          AAAA BBBBB")
            .build();

    private static final Address corporateAddress = Address.address()
            .withPostcode("AA2 5CC")
            .withPaon("          DDDD EEEEE")
            .build();

    private static final PoliceParentGuardian policeParentGuardian =
            PoliceParentGuardian.policeParentGuardian()
                    .withAddress(address)
                    .withBasePersonDetails(BasePersonDetail.basePersonDetail().withPersonName(PERSON_NAME).build()).
                    build();

    private static final String ORGANISATIONNAME = "OrganisationName";
    private static final String ORGANISATION_CRO_NUMBER = "CroNumber";
    private static final String ORGANISATION_PNCIDENTIFIER = "PnCidentifier";
    private static final String ORGANISATION_TELEPHONE = "TelephoneNumberBusiness";
    private static final String ORGANISATION_EMAIL = "EmailAddress1";


    @Test
    public void individualDefendant() {
        setUpIndividualDefendant();
        when(policeIndividualDefendant.getPnCidentifier()).thenReturn(PNC_IDENTIFIER);
        Defendant defendant = defendantConverter.convert(policeDefendant, getSpiInitialHearing());
        validate(defendant);
    }

    @Test
    public void individualDefendantWhenPncIdentifierOnlyExistsInPersonDefendant() {
        setUpIndividualDefendant();
        when(basePersonDefendant.getPnCidentifier()).thenReturn(PNC_IDENTIFIER);
        final Defendant defendant = defendantConverter.convert(policeDefendant, getSpiInitialHearing());
        validate(defendant);
    }

    private void setUpIndividualDefendant(){
        when(policeDefendant.getProsecutorReference()).thenReturn(PROSECUTOR_REFERENCE);
        when(policeDefendant.getId()).thenReturn(randomUUID());
        when(policeDefendant.getPoliceIndividualDefendant()).thenReturn(policeIndividualDefendant);
        when(policeIndividualDefendant.getAddress()).thenReturn(address);
        when(policeIndividualDefendant.getPersonDefendant()).thenReturn(basePersonDefendant);
        when(policeIndividualDefendant.getHearingLanguage()).thenReturn(HEARING_LANGUAGE);
        when(policeIndividualDefendant.getDocumentationLanguage()).thenReturn(DOCUMENTATION_LANGUAGE);
        when(policeIndividualDefendant.getLanguageNeeds()).thenReturn(LANGUAGE_NEEDS);
        when(policeIndividualDefendant.getSpecialNeeds()).thenReturn(SPECIAL_REQUIREMENTS);
        when(basePersonDefendant.getBasePersonDetails().getPersonName()).thenReturn(PERSON_NAME);
        when(policeIndividualDefendant.getParentGuardianDetails()).thenReturn(policeParentGuardian);
        when(policeIndividualDefendant.getAlias()).thenReturn(getMockAliases());
    }

    private void validate(final Defendant defendant){
        assertNotNull(defendant);
        assertThat(defendant.getAsn(), is(PROSECUTOR_REFERENCE));
        assertThat(defendant.getProsecutorDefendantReference(), is(PROSECUTOR_REFERENCE));
        assertThat(defendant.getHearingLanguage().name(), is(HEARING_LANGUAGE));
        assertThat(defendant.getDocumentationLanguage().name(), is(DOCUMENTATION_LANGUAGE));
        assertThat(defendant.getSpecificRequirements(), is(SPECIAL_REQUIREMENTS));
        assertThat(defendant.getLanguageRequirement(), is(LANGUAGE_NEEDS));
        assertThat(defendant.getIndividual(), isA(Individual.class));
        assertThat(defendant.getPncIdentifier(), is(PNC_IDENTIFIER));
        assertEquals(new HashSet<>(getExpectedAliases()), new HashSet<>(defendant.getIndividualAliases()));
    }

    private List<BasePersonName> getMockAliases() {
        final List<BasePersonName> aliases = new ArrayList<>();
        aliases.add(basePersonName()
                .withPersonTitle(PERSON_TITLE)
                .withPersonGivenName1(PERSON_GIVEN_NAME_1)
                .withPersonGivenName2(PERSON_GIVEN_NAME_2)
                .withPersonGivenName3(PERSON_GIVEN_NAME_3)
                .withPersonFamilyName(PERSON_FAMILY_NAME).build());

        aliases.add(basePersonName()
                .withPersonTitle(PERSON_TITLE)
                .withPersonGivenName1(PERSON_GIVEN_NAME_1)
                .withPersonGivenName2(PERSON_GIVEN_NAME_2)
                .withPersonFamilyName(PERSON_FAMILY_NAME).build());

        aliases.add(basePersonName()
                .withPersonTitle(PERSON_TITLE)
                .withPersonGivenName1(PERSON_GIVEN_NAME_1)
                .withPersonGivenName3(PERSON_GIVEN_NAME_3)
                .withPersonFamilyName(PERSON_FAMILY_NAME).build());

        aliases.add(basePersonName()
                .withPersonTitle(PERSON_TITLE)
                .withPersonGivenName1(PERSON_GIVEN_NAME_1)
                .withPersonFamilyName(PERSON_FAMILY_NAME).build());

        return aliases;

    }

    private List<IndividualAlias> getExpectedAliases() {
        final List<IndividualAlias> aliases = new ArrayList<>();
        aliases.add(individualAlias()
                .withTitle(PERSON_TITLE)
                .withFirstName(PERSON_GIVEN_NAME_1)
                .withGivenName2(PERSON_GIVEN_NAME_2)
                .withGivenName3(PERSON_GIVEN_NAME_3)
                .withLastName(PERSON_FAMILY_NAME)
                .build());
        aliases.add(individualAlias()
                .withTitle(PERSON_TITLE)
                .withFirstName(PERSON_GIVEN_NAME_1)
                .withGivenName2(PERSON_GIVEN_NAME_2)
                .withLastName(PERSON_FAMILY_NAME)
                .build());
        aliases.add(individualAlias()
                .withTitle(PERSON_TITLE)
                .withFirstName(PERSON_GIVEN_NAME_1)
                .withGivenName3(PERSON_GIVEN_NAME_3)
                .withLastName(PERSON_FAMILY_NAME)
                .build());
        aliases.add(individualAlias()
                .withTitle(PERSON_TITLE)
                .withFirstName(PERSON_GIVEN_NAME_1)
                .withLastName(PERSON_FAMILY_NAME)
                .build());

        return aliases;

    }

    private SpiInitialHearing getSpiInitialHearing() {
        return SpiInitialHearing.spiInitialHearing()
                .withDateOfHearing(now())
                .withCourtHearingLocation("Court location")
                .withTimeOfHearing("time")
                .build();
    }

    @Test
    public void corporateDefendant() {

        when(policeDefendant.getPoliceCorporateDefendant()).thenReturn(policeCorporateDefendant);
        when(policeDefendant.getId()).thenReturn(randomUUID());
        when(policeCorporateDefendant.getOrganisationName()).thenReturn(ORGANISATIONNAME);
        when(policeCorporateDefendant.getCrOnumber()).thenReturn(ORGANISATION_CRO_NUMBER);
        when(policeCorporateDefendant.getPnCidentifier()).thenReturn(ORGANISATION_PNCIDENTIFIER);
        when(policeCorporateDefendant.getTelephoneNumberBusiness()).thenReturn(ORGANISATION_TELEPHONE);
        when(policeCorporateDefendant.getEmailAddress1()).thenReturn(ORGANISATION_EMAIL);
        when(policeCorporateDefendant.getAddress()).thenReturn(corporateAddress);

        Defendant defendant = defendantConverter.convert(policeDefendant, getSpiInitialHearing());

        assertThat(defendant.getOrganisationName(), is(ORGANISATIONNAME));
        assertThat(defendant.getCroNumber(), is(ORGANISATION_CRO_NUMBER));
        assertThat(defendant.getPncIdentifier(), is(ORGANISATION_PNCIDENTIFIER));
        assertThat(defendant.getEmailAddress1(), is(ORGANISATION_EMAIL));
        assertNotNull(defendant.getAddress());

    }

}

