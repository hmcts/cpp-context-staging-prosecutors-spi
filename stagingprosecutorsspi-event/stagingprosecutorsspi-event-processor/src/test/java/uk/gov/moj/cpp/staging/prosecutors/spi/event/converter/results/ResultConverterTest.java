package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static java.math.BigInteger.valueOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.createPublicPoliceResultGenerated;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.getAssociatedPerson;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.getPerson;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.getXmlGregorianCalendar;

import uk.gov.dca.xmlschemas.libra.BaseEmailDetailStructure;
import uk.gov.dca.xmlschemas.libra.BaseHearingStructure;
import uk.gov.dca.xmlschemas.libra.BaseOffenceDetailStructure;
import uk.gov.dca.xmlschemas.libra.BasePersonDefendantStructure;
import uk.gov.dca.xmlschemas.libra.BasePersonDetailStructure;
import uk.gov.dca.xmlschemas.libra.BasePersonNameStructure;
import uk.gov.dca.xmlschemas.libra.BaseSimpleAddressStructure;
import uk.gov.dca.xmlschemas.libra.BaseTelephoneDetailStructure;
import uk.gov.dca.xmlschemas.libra.CourtAddressStructure;
import uk.gov.dca.xmlschemas.libra.CourtCaseStructure;
import uk.gov.dca.xmlschemas.libra.CourtCorporateDefendantStructure;
import uk.gov.dca.xmlschemas.libra.CourtDefendantStructure;
import uk.gov.dca.xmlschemas.libra.CourtHearingStructure;
import uk.gov.dca.xmlschemas.libra.CourtIndividualDefendantStructure;
import uk.gov.dca.xmlschemas.libra.CourtOffenceStructure;
import uk.gov.dca.xmlschemas.libra.CourtParentGuardianStructure;
import uk.gov.dca.xmlschemas.libra.CourtResultStructure;
import uk.gov.dca.xmlschemas.libra.CourtSessionStructure;
import uk.gov.dca.xmlschemas.libra.StdProsPoliceResultedCaseStructure;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.activiti.service.ReferenceDataQueryService;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Individual;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResultConverterTest {

    private static final ZonedDateTime SITTING_DAY = ZonedDateTime.of(LocalDate.of(2019, 2, 2), LocalTime.of(12, 3, 10), ZoneId.systemDefault());
    private static final Address ADDRESS = address().withAddress1("101 green house").withAddress2("address2").withAddress3("address3").withAddress4("address4").withPostcode("XE10 FR").build();
    private static final Address ADDRESS_1 = address().withAddress1("101 blue house").withAddress2("address2asdsadsadsadsadsadsadasdsad").withAddress4("address4").withPostcode("AE10 FR").build();

    @Mock
    private ReferenceDataQueryService referenceDataQueryService;

    @Spy
    private CourtDefendant courtDefendant;

    @Spy
    private CourtOffence courtOffence;

    @Spy
    private CourtCase courtCase;

    @Spy
    private CourtHearingSession courtHearingSession;

    @Spy
    private CourtSession courtSession;

    @Spy
    private HashMap<String, Object> context;

    @InjectMocks
    private ResultConverter resultConverter;

    @Test
    public void testConverter() {
        when(referenceDataQueryService.retrievePleaStatusCode(any())).thenReturn(Optional.of(1));
        setField(courtOffence, "referenceDataQueryService", referenceDataQueryService);
        setField(courtDefendant, "courtOffence", courtOffence);
        setField(courtCase, "courtDefendant", courtDefendant);
        setField(courtSession, "courtCase", courtCase);
        setField(courtSession, "courtHearingSession", courtHearingSession);

        context.put("aa746921-d839-4867-bcf9-b41db8ebc852", "London");

        final StdProsPoliceResultedCaseStructure stdProsPoliceResultedCaseStructure = resultConverter.convert(createPublicPoliceResultGenerated(), context);

        final CourtSessionStructure session = stdProsPoliceResultedCaseStructure.getSession();
        assertHearing(session.getCourtHearing());
        assertCases(session.getCase());
    }

    private void assertCases(final List<CourtCaseStructure> cases) {
        assertThat(cases, hasSize(1));
        cases.forEach(c -> assertCase(c));
    }

    private void assertCase(final CourtCaseStructure courtCaseStructure) {
        assertThat(courtCaseStructure.getPTIURN(), is("123445"));
        assertDefendant(courtCaseStructure.getDefendant());
    }

    private void assertDefendant(final CourtDefendantStructure courtCaseStructureDefendant) {
        assertThat(courtCaseStructureDefendant.getProsecutorReference(), is("prosecutorReference"));
        assertIndividualDefendant(courtCaseStructureDefendant.getCourtIndividualDefendant());
        assertCourtCorporateDefendant(courtCaseStructureDefendant.getCourtCorporateDefendant());
        assertOffenceList(courtCaseStructureDefendant.getOffence());
    }

    private void assertOffenceList(List<CourtOffenceStructure> offenceStructureList) {
        offenceStructureList.forEach(o -> assertOffence(o));
    }

    private void assertOffence(final CourtOffenceStructure courtOffenceStructure) {
        assertThat(courtOffenceStructure.getFinalDisposalIndicator(), is("T"));
        assertThat(courtOffenceStructure.getFinding(), is("finding"));
        assertThat(courtOffenceStructure.getModeOfTrial(), is(10));
        assertThat(courtOffenceStructure.getConvictionDate(), is(getXmlGregorianCalendar(LocalDate.of(2019, 2, 3))));
        assertThat(courtOffenceStructure.getInitiatedDate(), is(getXmlGregorianCalendar(LocalDate.of(2019, 2, 6))));
        assertThat(courtOffenceStructure.getConvictingCourt(), is(valueOf(1230)));
        assertOffenceDetailsStructure(courtOffenceStructure.getBaseOffenceDetails());

        courtOffenceStructure.getResult().forEach(r -> assertResult(r));
    }

    private void assertResult(final CourtResultStructure courtResultStructure) {
        assertThat(courtResultStructure.getResultText(), is("resultText"));
        assertThat(courtResultStructure.getResultCode(), is(1234));
    }

    private void assertOffenceDetailsStructure(final BaseOffenceDetailStructure baseOffenceDetails) {
        assertThat(baseOffenceDetails.getOffenceSequenceNumber(), is(1));
        assertThat(baseOffenceDetails.getOffenceCode(), is("61131"));
        assertThat(baseOffenceDetails.getOffenceWording(), is("wording"));
        assertThat(baseOffenceDetails.getChargeDate(), is(getXmlGregorianCalendar(LocalDate.of(2019, 2, 5))));
        assertThat(baseOffenceDetails.getArrestDate(), is(getXmlGregorianCalendar(LocalDate.of(2019, 2, 8))));

        assertThat(baseOffenceDetails.getOffenceTiming().getOffenceDateCode(), is(valueOf(1998)));
        assertThat(baseOffenceDetails.getOffenceTiming().getOffenceEnd().getOffenceEndDate(), is(getXmlGregorianCalendar(LocalDate.of(2019, 2, 7))));
        assertThat(baseOffenceDetails.getOffenceTiming().getOffenceStart().getOffenceDateStartDate(), is(getXmlGregorianCalendar(LocalDate.of(2019, 2, 6))));

        assertThat(baseOffenceDetails.getAlcoholRelatedOffence().getAlcoholLevelMethod(), is("AlcoholReadingMethod"));
        assertThat(baseOffenceDetails.getAlcoholRelatedOffence().getAlcoholLevelAmount(), is(1));

        assertThat(baseOffenceDetails.getVehicleRelatedOffence().getVehicleRegistrationMark(), is("34334"));
        assertThat(baseOffenceDetails.getVehicleRelatedOffence().getVehicleCode(), is("L"));
        assertThat(baseOffenceDetails.getLocationOfOffence(), is("London"));

    }

    private void assertCourtCorporateDefendant(final CourtCorporateDefendantStructure courtCorporateDefendant) {
        assertThat(courtCorporateDefendant.getBailStatus(), is("A"));
        assertThat(courtCorporateDefendant.getPNCidentifier(), is("pncId"));
        assertThat(courtCorporateDefendant.getPresentAtHearing(), is("F"));
        assertThat(courtCorporateDefendant.getTelephoneNumberBusiness(), is("5465767"));
        assertAddress(courtCorporateDefendant.getAddress(), ADDRESS);
        assertEmailDetailStructure(courtCorporateDefendant.getEmailDetails(), "abc@com.uk", "xyx@com.uk");
        assertThat(courtCorporateDefendant.getOrganisationName().getOrganisationName(), is("corporateDefendant"));
    }

    private void assertIndividualDefendant(final CourtIndividualDefendantStructure courtIndividualDefendant) {
        assertThat(courtIndividualDefendant.getReasonForBailConditionsOrCustody(), is("reason"));
        assertThat(courtIndividualDefendant.getBailStatus(), is("A"));
        assertThat(courtIndividualDefendant.getPresentAtHearing(), is("T"));
        assertAddress(courtIndividualDefendant.getAddress(), ADDRESS);
        assertPersonDefendant(courtIndividualDefendant.getPersonDefendant());
        assertParentGuardian(courtIndividualDefendant.getParentGuardianDetails());
    }

    private void assertParentGuardian(final CourtParentGuardianStructure parentGuardianDetails) {
        assertAddress(parentGuardianDetails.getAddress(), ADDRESS_1);
        assertPersonDetails(parentGuardianDetails.getBasePersonDetails(), getAssociatedPerson(), 2);
    }

    private void assertPersonDefendant(final BasePersonDefendantStructure personDefendant) {
        assertThat(personDefendant.getBailConditions(), is("bailCondition"));
        assertThat(personDefendant.getPersonStatedNationality(), is(nullValue()));
        assertThat(personDefendant.getPNCidentifier(), is("pncId"));
        assertPersonDetails(personDefendant.getBasePersonDetails(), getPerson(), 1);
    }

    private void assertPersonDetails(final BasePersonDetailStructure basePersonDetails, final Individual individual, final Integer gender) {
        assertThat(basePersonDetails.getBirthdate(), is(getXmlGregorianCalendar(individual.getDateOfBirth())));
        assertThat(basePersonDetails.getGender(), is(gender.byteValue()));
        final ContactNumber contact = individual.getContact();
        assertEmailDetailStructure(basePersonDetails.getEmailDetails(), contact.getPrimaryEmail(), contact.getSecondaryEmail());
        assertTelephoneDetailStructure(basePersonDetails.getTelephoneDetails(), contact.getHome(), contact.getMobile(), contact.getWork());
        assertPerson(basePersonDetails.getPersonName(), individual);
    }

    private void assertPerson(final BasePersonNameStructure personName, final Individual individual) {
        assertThat(personName.getPersonGivenName1(), is(individual.getFirstName()));
        assertThat(personName.getPersonGivenName2(), is(individual.getMiddleName()));
        assertThat(personName.getPersonFamilyName(), is(individual.getLastName()));
        assertThat(personName.getPersonTitle(), is(individual.getTitle().toString()));
    }

    private void assertTelephoneDetailStructure(final BaseTelephoneDetailStructure telephoneDetails, final String home, final String mobile, final String work) {
        assertThat(telephoneDetails.getTelephoneNumberBusiness(), is(work));
        assertThat(telephoneDetails.getTelephoneNumberHome(), is(home));
        assertThat(telephoneDetails.getTelephoneNumberMobile(), is(mobile));
    }

    private void assertEmailDetailStructure(final BaseEmailDetailStructure emailDetails, final String primaryEmail, final String secondaryEmail) {
        assertThat(emailDetails.getEmailAddress1(), is(primaryEmail));
        assertThat(emailDetails.getEmailAddress2(), is(secondaryEmail));
    }

    private void assertAddress(final CourtAddressStructure address, final Address primaryAddress) {
        final BaseSimpleAddressStructure simpleAddress = address.getSimpleAddress();
        assertThat(simpleAddress, is(notNullValue()));
        assertThat(simpleAddress.getAddressLine1(), is(primaryAddress.getAddress1()));
        assertThat(simpleAddress.getAddressLine2(), is(primaryAddress.getAddress2()));
        assertThat(simpleAddress.getAddressLine3(), is(primaryAddress.getAddress3()));
        assertThat(simpleAddress.getAddressLine4(), is(primaryAddress.getAddress4()));
        assertThat(simpleAddress.getAddressLine5(), is(primaryAddress.getPostcode()));
    }

    private void assertHearing(final CourtHearingStructure courtHearing) {
        assertThat(courtHearing.getPSAcode(), is(valueOf(1230)));
        final BaseHearingStructure hearing = courtHearing.getHearing();
        assertThat(hearing.getCourtHearingLocation(), is("COURT12345"));
        assertThat(hearing.getDateOfHearing(), is(getXmlGregorianCalendar(SITTING_DAY)));
        assertThat(hearing.getTimeOfHearing(), is(getXmlGregorianCalendar(SITTING_DAY)));
    }

}