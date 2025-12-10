package uk.gov.moj.cpp.staging.prosecutors.spi.event.helper;

import static com.google.common.collect.ImmutableList.of;
import static java.time.LocalDate.now;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.xml.datatype.DatatypeFactory.newInstance;
import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.justice.core.courts.AllocationDecision.allocationDecision;
import static uk.gov.justice.core.courts.ContactNumber.contactNumber;
import static uk.gov.justice.core.courts.Gender.FEMALE;
import static uk.gov.justice.core.courts.Gender.MALE;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;
import static uk.gov.justice.core.courts.JudicialResultPrompt.judicialResultPrompt;
import static uk.gov.justice.core.courts.JudicialResultPromptDurationElement.judicialResultPromptDurationElement;
import static uk.gov.justice.core.courts.OffenceFacts.offenceFacts;
import static uk.gov.justice.core.courts.Plea.plea;
import static uk.gov.justice.core.courts.SecondaryCJSCode.secondaryCJSCode;
import static uk.gov.justice.core.courts.VehicleCode.LARGE_GOODS_VEHICLE;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.PublicPoliceResultGenerated.publicPoliceResultGenerated;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.AssociatedIndividual.associatedIndividual;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.AttendanceDay.attendanceDay;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseDefendant.caseDefendant;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CourtCentre.courtCentre;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CourtCentreWithLJA.courtCentreWithLJA;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.IndividualDefendant.individualDefendant;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.OffenceDetails.offenceDetails;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.OrganisationDetails.organisationDetails;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SessionDay.sessionDay;

import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.BailStatus;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.justice.core.courts.Gender;
import uk.gov.justice.core.courts.IndicatedPlea;
import uk.gov.justice.core.courts.IndicatedPleaValue;
import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.JudicialResultPromptDurationElement;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.OffenceFacts;
import uk.gov.justice.core.courts.SecondaryCJSCode;
import uk.gov.justice.core.courts.VehicleCode;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.AssociatedIndividual;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.AttendanceDay;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.AttendanceType;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CaseDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CourtCentre;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.CourtCentreWithLJA;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Individual;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.IndividualDefendant;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.OffenceDetails;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.OrganisationDetails;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.SessionDay;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.UUID;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.lang3.RandomStringUtils;

public class TestTemplate {
    private static final String RESULT_TEXT_3000 = RandomStringUtils.randomAlphabetic(3000);
    public static final String RESULT_TEXT_2500 = RESULT_TEXT_3000.substring(0, 2500);
    private static final String FIRST_NAME_1 = "Jane";
    private static final String LAST_NAME_1 = "Johnson";
    private static final Gender GENDER_1 = FEMALE;
    private static final LocalDate DATE_OF_BIRTH_1 = LocalDate.of(1980, 11, 15);
    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Smith";
    private static final Gender GENDER = MALE;
    private static final LocalDate DATE_OF_BIRTH = LocalDate.of(1970, 1, 10);
    private static final String TITLE = "Baron";
    private static final String TITLE_MR = "Mr";
    private static final String TITLE_MRS = "Mrs";
    private static final Address ADDRESS = address().withAddress1("101 green house").withAddress2("address2").withAddress3("address3").withAddress4("address4").withPostcode("XE10 FR").build();
    private static final Address ADDRESS_1 = address().withAddress1("101 blue house").withAddress2("address2asdsadsadsadsadsadsadasdsadasdsadasdsadsasadsdsadddd").withAddress4("address4").withPostcode("AE10 FR").build();
    private static final ContactNumber CONTACT_NUMBER = contactNumber().withFax("454645").withHome("56567").withMobile("123232").withWork("5465767").withPrimaryEmail("abc@com.uk").withSecondaryEmail("xyx@com.uk").build();
    private static final ContactNumber CONTACT_NUMBER_1 = contactNumber().withFax("12345").withHome("5678").withMobile("91010").withWork("1233").withPrimaryEmail("efg@com.uk").withSecondaryEmail("ljk@com.uk").build();
    private static final UUID CASE_ID = randomUUID();
    private static final String URN = "123445";
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final ZonedDateTime SITTING_DAY = ZonedDateTime.of(LocalDate.of(2019, 2, 2), LocalTime.of(12, 3, 10), ZoneId.systemDefault());
    public static final LocalDate LOCAL_DATE = LocalDate.of(2019, 5, 14);
    public static final LocalDate HEARING_DATE = LocalDate.of(2019, 5, 19);
    private static final UUID DEFAULT_DEFENDANT_ID1 = fromString("dddd1111-1e20-4c21-916a-81a6c90239e5");
    private static final UUID AMENDMANT_REASON_ID = fromString("dddd2222-1e20-4c21-916a-81a6c90239e5");
    private static final UUID ORDERED_HEARING_ID = fromString("dddd4444-1e20-4c21-916a-81a6c90239e5");
    private static final UUID ID = fromString("dddd1111-1e20-4c21-916a-81a6c90239e5");
    private static final Integer PSA_CODE = 1010;
    private static final String MIDDLE_NAME = "middleName";
    private static final String GUILTY = "GUILTY";
    private static final String SECONDARY_CJS_CODE_1 = "3050";
    private static final String SECONDARY_CJS_CODE_2 = "3074";
    private static final String SECONDARY_CJS_TEXT_2 = "Disqualified until extended test passed";
    private static final String SECONDARY_CJS_CODE_3 = "";
    private static final String SECONDARY_CJS_TEXT_3 = "NonNumericSecondaryCjsCode";

    public static PublicPoliceResultGenerated createPublicPoliceResultGenerated() {
        final PublicPoliceResultGenerated publicPoliceResultGenerated = publicPoliceResultGenerated();
        publicPoliceResultGenerated.setId(randomUUID());
        publicPoliceResultGenerated.setCaseId(CASE_ID);
        publicPoliceResultGenerated.setUrn(URN);
        publicPoliceResultGenerated.setCourtCentreWithLJA(courtCentreWithLJA().withPsaCode(1230).withCourtHearingLocation("COURT12345").build());
        publicPoliceResultGenerated.setDefendant(caseDefendant()
                .withDefendantId(DEFENDANT_ID)
                .withPncId("pncId")
                .withProsecutorReference("prosecutorReference")
                .withIndividualDefendant(individualDefendant()
                        .withBailConditions("bailCondition")
                        .withBailStatus(BailStatus.bailStatus().withCode("A").withDescription("desc").withId(randomUUID()).build())
                        .withPresentAtHearing("T")
                        .withReasonForBailConditionsOrCustody("reason")
                        .withPerson(getPerson())
                        .build())
                .withAssociatedPerson(of(associatedIndividual()
                        .withPerson(getAssociatedPerson())
                        .withRole("parentGuardian")
                        .build()))
                .withCorporateDefendant(organisationDetails()
                        .withAddress(ADDRESS)
                        .withContact(CONTACT_NUMBER)
                        .withIncorporationNumber("incorporationNumber")
                        .withName("corporateDefendant")
                        .withRegisteredCharityNumber("registeredCharityNumber")
                        .withPresentAtHearing("F")
                        .build())
                .withOffences(of(offenceDetails()
                        .withPlea(plea()
                                .withPleaDate(LocalDate.of(2019, 1, 2))
                                .withPleaValue(GUILTY)
                                .build())
                        .withConvictionDate(LocalDate.of(2019, 2, 3))
                        .withConvictingCourt(123)
                        .withModeOfTrial("3")
                        .withFinalDisposal("T")
                        .withArrestDate(LocalDate.of(2019, 2, 8))
                        .withChargeDate(LocalDate.of(2019, 2, 5))
                        .withOffenceCode("61131")
                        .withOffenceDateCode(2018 - 10 - 10)
                        .withEndDate(LocalDate.of(2019, 2, 7))
                        .withId(fromString("aa746921-d839-4867-bcf9-b41db8ebc852"))
                        .withOffenceSequenceNumber(1)
                        .withStartDate(LocalDate.of(2019, 2, 6))
                        .withWording("wording")
                        .withFinding("finding")
                        .withOffenceFacts(offenceFacts().withAlcoholReadingAmount(1).withAlcoholReadingMethodCode("AlcoholReadingMethod")
                                .withVehicleCode(LARGE_GOODS_VEHICLE)
                                .withVehicleRegistration("34334")
                                .build())
                        .withJudicialResults(of(judicialResult().withCjsCode("1234").withLabel("12123").withResultText("resultText").build()))
                        .withAllocationDecision(allocationDecision().withMotReasonCode("10").withOffenceId(randomUUID()).withMotReasonDescription("Remand Custody").build())
                        .build()))
                .withAttendanceDays(of(AttendanceDay.attendanceDay().withDay(now()).withAttendanceType(AttendanceType.IN_PERSON).build()))
                .withJudicialResults(of(judicialResult().build()))
                .build());
        publicPoliceResultGenerated.setSessionDays(of(sessionDay().withListedDurationMinutes(10).withListingSequence(10).withSittingDay(SITTING_DAY).build()));
        return publicPoliceResultGenerated;
    }

    public static Individual getAssociatedPerson() {
        return createPerson(FIRST_NAME_1, LAST_NAME_1, GENDER_1, DATE_OF_BIRTH_1, TITLE, ADDRESS_1, CONTACT_NUMBER_1);
    }

    public static Individual getPerson() {
        return createPerson(FIRST_NAME, LAST_NAME, GENDER, DATE_OF_BIRTH, TITLE_MRS, ADDRESS, CONTACT_NUMBER);
    }

    private static Individual createPerson(final String firstName, final String lastName, final Gender gender, final LocalDate dateOfBirth, final String title, final Address address, final ContactNumber contactNumber) {
        return individual()
                .withFirstName(firstName)
                .withLastName(lastName)
                .withGender(gender)
                .withDateOfBirth(dateOfBirth)
                .withTitle(title)
                .withMiddleName("middle")
                .withNationality("UK")
                .withAddress(address)
                .withContact(contactNumber)
                .build();
    }

    public static XMLGregorianCalendar getXmlGregorianCalendar(final LocalDate localDate) {
        XMLGregorianCalendar xmlGregorianCalendar = null;
        try {
            xmlGregorianCalendar = newInstance().newXMLGregorianCalendar(localDate.toString());
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        return xmlGregorianCalendar;
    }


    public static XMLGregorianCalendar getXmlGregorianCalendar(final ZonedDateTime zonedDateTime) {
        final GregorianCalendar gregorianCalendar = GregorianCalendar.from(zonedDateTime);
        XMLGregorianCalendar xmlGregorianCalendar = null;
        try {
            xmlGregorianCalendar = newInstance().newXMLGregorianCalendar(gregorianCalendar);
            xmlGregorianCalendar.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
            xmlGregorianCalendar.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
        return xmlGregorianCalendar;
    }


    public static CaseDefendant buildCaseDefendant() {
        return caseDefendant()
                .withAssociatedPerson(of(buildAssociatedIndividual()))
                .withAttendanceDays(buildListOfAttendanceDays())
                .withPncId("pncId")
                .withProsecutorReference("prosecutorReference")
                .withCorporateDefendant(buildCorporateDefendant())
                .withDefendantId(DEFAULT_DEFENDANT_ID1)
                .withIndividualDefendant(buildIndividualDefendant())
                .withJudicialResults(buildListOfJudicialResults())
                .withOffences(buildListOfOffences())
                .build();
    }

    public static CaseDefendant buildCaseDefendantV1() {
        return caseDefendant()
                .withAssociatedPerson(of(buildAssociatedIndividual()))
                .withAttendanceDays(buildListOfAttendanceDays())
                .withPncId("pncId")
                .withProsecutorReference("prosecutorReference")
                .withCorporateDefendant(buildCorporateDefendant())
                .withDefendantId(DEFAULT_DEFENDANT_ID1)
                .withIndividualDefendant(buildIndividualDefendantV1())
                .withJudicialResults(buildListOfJudicialResults())
                .withOffences(buildListOfOffences())
                .build();
    }

    public static CaseDefendant buildCaseDefendantV2() {
        return caseDefendant()
                .withAssociatedPerson(of(buildAssociatedIndividual()))
                .withAttendanceDays(buildListOfAttendanceDays())
                .withPncId("pncId")
                .withProsecutorReference("prosecutorReference")
                .withCorporateDefendant(buildCorporateDefendant())
                .withDefendantId(DEFAULT_DEFENDANT_ID1)
                .withIndividualDefendant(buildIndividualDefendantV2())
                .withJudicialResults(buildListOfJudicialResults())
                .withOffences(buildListOfOffences())
                .build();
    }

    public static CaseDefendant buildCaseDefendantV3() {
        return caseDefendant()
                .withAssociatedPerson(of(buildAssociatedIndividual()))
                .withAttendanceDays(buildListOfAttendanceDays())
                .withPncId("pncId")
                .withProsecutorReference("prosecutorReference")
                .withCorporateDefendant(buildCorporateDefendant())
                .withDefendantId(DEFAULT_DEFENDANT_ID1)
                .withIndividualDefendant(buildIndividualDefendantV3())
                .withJudicialResults(buildListOfJudicialResults())
                .withOffences(buildListOfOffences())
                .build();
    }

    public static CaseDefendant buildCaseDefendantNoBailConditions() {
        return caseDefendant()
                .withAssociatedPerson(of(buildAssociatedIndividual()))
                .withAttendanceDays(buildListOfAttendanceDays())
                .withPncId("pncId")
                .withProsecutorReference("prosecutorReference")
                .withCorporateDefendant(buildCorporateDefendant())
                .withDefendantId(DEFAULT_DEFENDANT_ID1)
                .withIndividualDefendant(buildIndividualDefendantNoBailConditions())
                .withJudicialResults(buildListOfJudicialResults())
                .withOffences(buildListOfOffences())
                .build();
    }


    public static AssociatedIndividual buildAssociatedIndividual() {
        return associatedIndividual()
                .withPerson(buildIndividualPerson())
                .withRole("parentGuardian")
                .build();
    }

    public static List<OffenceDetails> buildListOfOffences() {
        List<OffenceDetails> offenceDetails = new ArrayList<>();
        offenceDetails.add(buildOffenceDetailsWithMotReasonCode("10", GUILTY));
        return offenceDetails;
    }

    public static OffenceDetails buildOffenceDetailsWithMotReasonCode(final String motReasonCode, final String pleaValue) {
        return offenceDetails()
                .withArrestDate(LOCAL_DATE)
                .withChargeDate(LOCAL_DATE)
                .withConvictingCourt(20)
                .withConvictionDate(LOCAL_DATE)
                .withEndDate(LOCAL_DATE)
                .withFinalDisposal("finalDisposal")
                .withFinding("finding")
                .withId(ID)
                .withJudicialResults(buildListOfJudicialResults())
                .withModeOfTrial("10")
                .withAllocationDecision(allocationDecision().withMotReasonCode(motReasonCode).withSequenceNumber(20).withOffenceId(randomUUID()).build())
                .withOffenceCode("123456")
                .withOffenceDateCode(27)
                .withOffenceFacts(buildOffenceFacts())
                .withOffenceSequenceNumber(12345)
                .withFinding("finding")
                .withFinalDisposal("finalDisposal")
                .withPlea(plea()
                        .withOffenceId(ID)
                        .withOriginatingHearingId(ORDERED_HEARING_ID)
                        .withPleaDate(LOCAL_DATE)
                        .withPleaValue(pleaValue)
                        .build())
                .withStartDate(LOCAL_DATE)
                .withWording("wording")
                .build();
    }

    public static OffenceDetails buildOffenceDetailsWithIndicatedPlea(final String motReasonCode, final IndicatedPleaValue indicatedPleaValue) {
        return offenceDetails()
                .withArrestDate(LOCAL_DATE)
                .withChargeDate(LOCAL_DATE)
                .withConvictingCourt(20)
                .withConvictionDate(LOCAL_DATE)
                .withEndDate(LOCAL_DATE)
                .withFinalDisposal("finalDisposal")
                .withFinding("finding")
                .withId(ID)
                .withJudicialResults(buildListOfJudicialResults())
                .withModeOfTrial("10")
                .withAllocationDecision(allocationDecision().withMotReasonCode(motReasonCode).withSequenceNumber(20).withOffenceId(randomUUID()).build())
                .withOffenceCode("123456")
                .withOffenceDateCode(27)
                .withOffenceFacts(buildOffenceFacts())
                .withOffenceSequenceNumber(12345)
                .withFinding("finding")
                .withFinalDisposal("finalDisposal")
                .withIndicatedPlea(IndicatedPlea.indicatedPlea()
                        .withOffenceId(ID)
                        .withOriginatingHearingId(ORDERED_HEARING_ID)
                        .withIndicatedPleaDate(LOCAL_DATE)
                        .withIndicatedPleaValue(indicatedPleaValue)
                        .build())
                .withStartDate(LOCAL_DATE)
                .withWording("wording")
                .build();
    }

    public static OffenceDetails buildOffenceDetailsWithConvictionDate( final LocalDate convictionDate) {
        return offenceDetails()
                .withArrestDate(LOCAL_DATE)
                .withChargeDate(LOCAL_DATE)
                .withConvictingCourt(20)
                .withConvictionDate(convictionDate)
                .withEndDate(LOCAL_DATE)
                .withFinalDisposal("finalDisposal")
                .withFinding("finding")
                .withId(ID)
                .withJudicialResults(buildListOfJudicialResults())
                .withModeOfTrial("10")
                .withAllocationDecision(allocationDecision().withMotReasonCode("10").withSequenceNumber(20).withOffenceId(randomUUID()).build())
                .withOffenceCode("123456")
                .withOffenceDateCode(27)
                .withOffenceFacts(buildOffenceFacts())
                .withOffenceSequenceNumber(12345)
                .withFinding("finding")
                .withFinalDisposal("finalDisposal")
                .withIndicatedPlea(IndicatedPlea.indicatedPlea()
                        .withOffenceId(ID)
                        .withOriginatingHearingId(ORDERED_HEARING_ID)
                        .withIndicatedPleaDate(LOCAL_DATE)
                        .withIndicatedPleaValue(IndicatedPleaValue.INDICATED_GUILTY)
                        .build())
                .withStartDate(LOCAL_DATE)
                .withWording("wording")
                .build();
    }

    public static OffenceDetails buildOffenceDetailsWithJudicialResults(final List<JudicialResult> judicialResults) {
        return offenceDetails()
                .withArrestDate(LOCAL_DATE)
                .withChargeDate(LOCAL_DATE)
                .withConvictingCourt(20)
                .withConvictionDate(LOCAL_DATE)
                .withEndDate(LOCAL_DATE)
                .withFinalDisposal("finalDisposal")
                .withFinding("finding")
                .withId(ID)
                .withJudicialResults(judicialResults)
                .withModeOfTrial("10")
                .withOffenceCode("123456")
                .withOffenceDateCode(27)
                .withOffenceFacts(buildOffenceFacts())
                .withOffenceSequenceNumber(12345)
                .withFinding("finding")
                .withFinalDisposal("finalDisposal")
                .withPlea(plea()
                        .withOffenceId(ID)
                        .withOriginatingHearingId(ORDERED_HEARING_ID)
                        .withPleaDate(LOCAL_DATE)
                        .withPleaValue("pleaValue")
                        .build())
                .withStartDate(LOCAL_DATE)
                .withWording("wording")
                .build();
    }

    public static OffenceDetails buildOffenceDetailsWithMotReasonCode(final String motReasonCode) {
        return offenceDetails()
                .withArrestDate(LOCAL_DATE)
                .withChargeDate(LOCAL_DATE)
                .withConvictingCourt(20)
                .withConvictionDate(LOCAL_DATE)
                .withEndDate(LOCAL_DATE)
                .withFinalDisposal("finalDisposal")
                .withFinding("finding")
                .withId(ID)
                .withJudicialResults(buildListOfJudicialResults())
                .withModeOfTrial("10")
                .withAllocationDecision(allocationDecision().withMotReasonCode(motReasonCode).withSequenceNumber(20).withOffenceId(randomUUID()).build())
                .withOffenceCode("123456")
                .withOffenceDateCode(27)
                .withOffenceFacts(buildOffenceFacts())
                .withOffenceSequenceNumber(12345)
                .withFinding("finding")
                .withFinalDisposal("finalDisposal")
                .withStartDate(LOCAL_DATE)
                .withWording("wording")
                .build();
    }


    public static OffenceDetails buildOffenceDetailsWithSecondaryCjsCodes() {
        return offenceDetails()
                .withArrestDate(LOCAL_DATE)
                .withChargeDate(LOCAL_DATE)
                .withConvictingCourt(20)
                .withConvictionDate(LOCAL_DATE)
                .withEndDate(LOCAL_DATE)
                .withFinalDisposal("finalDisposal")
                .withFinding("finding")
                .withId(ID)
                .withJudicialResults(buildListOfJudicialResultsWithSecondaryCjsCodes())
                .withModeOfTrial("10")
                .withAllocationDecision(allocationDecision().withMotReasonCode("10").withSequenceNumber(20).withOffenceId(randomUUID()).build())
                .withOffenceCode("123456")
                .withOffenceDateCode(27)
                .withOffenceFacts(buildOffenceFacts())
                .withOffenceSequenceNumber(12345)
                .withFinding("finding")
                .withFinalDisposal("finalDisposal")
                .withPlea(plea()
                        .withOffenceId(ID)
                        .withOriginatingHearingId(ORDERED_HEARING_ID)
                        .withPleaDate(LOCAL_DATE)
                        .withPleaValue(GUILTY)
                        .build())
                .withStartDate(LOCAL_DATE)
                .withWording("wording")
                .build();
    }

    public static OffenceFacts buildOffenceFacts() {
        return offenceFacts()
                .withAlcoholReadingAmount(200)
                .withAlcoholReadingMethodCode("a")
                .withVehicleCode(VehicleCode.LARGE_GOODS_VEHICLE)
                .withVehicleRegistration("a")
                .build();
    }

    public static List<JudicialResult> buildListOfJudicialResults() {
        List<JudicialResult> judicialResults = new ArrayList<>();
        judicialResults.add(
                buildJudicialResult(true, true)
        );
        return judicialResults;
    }

    public static List<JudicialResult> buildListOfJudicialResultsWithSecondaryCjsCodes() {
        List<JudicialResult> judicialResults = new ArrayList<>();
        judicialResults.add(
                buildJudicialResult(true, true, true, true)
        );
        return judicialResults;
    }

    public static JudicialResult buildJudicialResult(final boolean withNextHearingObject, final boolean withNextHearingListedStartDateTime) {
        return buildJudicialResult(withNextHearingObject, withNextHearingListedStartDateTime, true, false);
    }

    public static JudicialResult buildJudicialResult(final boolean withNextHearingObject, final boolean withNextHearingListedStartDateTime,
                                                     final boolean withNextHearingLocation, final boolean withSecondaryCjsCodes, final boolean withTotalPenaltyPoints, final boolean withIsFinancialImposition, final boolean withDurationElement, final int primaryDurationUnit) {
        return buildJudicialResult(withNextHearingObject, withNextHearingListedStartDateTime, withNextHearingLocation, withSecondaryCjsCodes, withTotalPenaltyPoints, withIsFinancialImposition, withDurationElement, true, true, primaryDurationUnit);
    }

    public static JudicialResult buildJudicialResult(final boolean withNextHearingObject, final boolean withNextHearingListedStartDateTime,
                                                     final boolean withNextHearingLocation, final boolean withSecondaryCjsCodes, final boolean withTotalPenaltyPoints, final boolean withIsFinancialImposition, final boolean withDurationElement,
                                                     final boolean withDurationStartDate, final boolean withDurationEndDate, final int primaryDurationUnit) {
        final JudicialResult.Builder judicialResultBuilder = judicialResult();

        JudicialResultPromptDurationElement durationElement = null;
        if (withDurationElement) {
            durationElement = buildJudicialResultPromptDurationElement(withDurationStartDate, withDurationEndDate, primaryDurationUnit);
        }

        judicialResultBuilder
                .withAmendmentDate(LOCAL_DATE)
                .withAmendmentReason("amendmantReason")
                .withAmendmentReasonId(AMENDMANT_REASON_ID)
                .withApprovedDate(LOCAL_DATE)
                .withCategory(JudicialResultCategory.ANCILLARY)
                .withOrderedDate(LOCAL_DATE)
                .withLastSharedDateTime("20190112T110000Z")
                .withIsAdjournmentResult(Boolean.TRUE)
                .withIsAvailableForCourtExtract(Boolean.TRUE)
                .withIsConvictedResult(Boolean.TRUE)
                .withIsFinancialResult(Boolean.TRUE)
                .withJudicialResultPrompts(buildListOfJudicialPrompts(withTotalPenaltyPoints, withIsFinancialImposition, durationElement))
                .withCjsCode("12345")
                .withLabel("label")
                .withWelshLabel("WS")
                .withResultText(RESULT_TEXT_3000)
                .withOrderedHearingId(ORDERED_HEARING_ID)
                .withUsergroups(Arrays.asList("userGroup1", "userGroup2"))
                .withRank(BigDecimal.ONE)
                .withQualifier("qualifier,BT,C,qual1,qual2,qual3,qual14,FH,T")
                //.withQualifier("BT")
                .withPostHearingCustodyStatus("C")
                .withDurationElement(durationElement);

        if (withSecondaryCjsCodes) {
            judicialResultBuilder.withSecondaryCJSCodes(buildListOfSecondaryCJSCodes());
        }

        if (withNextHearingObject) {
            final NextHearing.Builder builder = NextHearing.nextHearing()
                    .withReportingRestrictionReason("reportingRestrictionReason")
                    .withAdjournmentReason("Unexpected circumstances meant that the case could not be heard For a live link hearing");
            if (withNextHearingLocation) {
                builder.withCourtCentre(uk.gov.justice.core.courts.CourtCentre.courtCentre()
                        .withPsaCode(1230).withCourtHearingLocation("COURT12345")
                        .build());
            }
            if (withNextHearingListedStartDateTime) {
                builder.withListedStartDateTime(SITTING_DAY);
            }
            judicialResultBuilder.withNextHearing(builder.build());
        }
        return judicialResultBuilder.build();
    }

    public static JudicialResult buildJudicialResult(final boolean withNextHearingObject, final boolean withNextHearingListedStartDateTime,
                                                     final boolean withNextHearingLocation, final boolean withSecondaryCjsCodes) {
        return buildJudicialResult(withNextHearingObject, withNextHearingListedStartDateTime, withNextHearingLocation, withSecondaryCjsCodes, true, true, true, 1);
    }

    private static List<SecondaryCJSCode> buildListOfSecondaryCJSCodes() {
        return of(
                secondaryCJSCode().withCjsCode(SECONDARY_CJS_CODE_1).withText(RESULT_TEXT_3000).build(),
                secondaryCJSCode().withCjsCode(SECONDARY_CJS_CODE_2).withText(SECONDARY_CJS_TEXT_2).build(),
                secondaryCJSCode().withCjsCode(SECONDARY_CJS_CODE_3).withText(SECONDARY_CJS_TEXT_3).build()
        );
    }

    private static JudicialResultPromptDurationElement buildJudicialResultPromptDurationElement(final boolean withDurationStartDate, final boolean withDurationEndDate, final int primaryDurationUnit) {
        String durationStartDate = withDurationStartDate ? "19/09/2019" : null;
        String durationEndState = withDurationEndDate ? "19/09/2005" : null;

        return judicialResultPromptDurationElement()
                .withPrimaryDurationUnit("L")
                .withPrimaryDurationValue(primaryDurationUnit)
                .withSecondaryDurationUnit("M")
                .withSecondaryDurationValue(2)
                .withDurationStartDate(durationStartDate)
                .withDurationEndDate(durationEndState)
                .build();
    }

    public static List<JudicialResultPrompt> buildListOfJudicialPrompts(final boolean withTotalPenaltyPoints, final boolean withIsFinancialImposition, final JudicialResultPromptDurationElement durationElement) {
        List<JudicialResultPrompt> judicialResultPrompts = new ArrayList<>();
        final JudicialResultPrompt.Builder builder = judicialResultPrompt();
        builder.withCourtExtract("Y")
                .withLabel("label")
                .withDurationSequence(1)
                .withPromptReference("")
                .withPromptSequence(BigDecimal.ONE)
                .withUsergroups(Arrays.asList("GROUP1", "GROUP2"))
                .withValue("£10")
                .withWelshLabel("WS")
                .withDurationElement(durationElement);
        if (withTotalPenaltyPoints) {
            builder.withTotalPenaltyPoints(new BigDecimal(12));
        }
        if (withIsFinancialImposition) {
            builder.withIsFinancialImposition(true);
        }
        judicialResultPrompts.add(builder.build());

        final JudicialResultPrompt.Builder builder1 = judicialResultPrompt();
        builder1.withCourtExtract("Y")
                .withLabel("Concurrent")
                .withDurationSequence(1)
                .withPromptReference("concurrent")
                .withPromptSequence(BigDecimal.TEN)
                .withUsergroups(Arrays.asList("GROUP1", "GROUP2", "GROUP3"))
                .withType("BOOLEAN")
                .withValue("false")
                .withQualifier("C")
                .withWelshLabel("Cydamserol")
                .withDurationElement(durationElement);
        judicialResultPrompts.add(builder1.build());

        final JudicialResultPrompt.Builder builder2 = judicialResultPrompt();
        builder2.withCourtExtract("Y")
                .withLabel("Non Boolean")
                .withDurationSequence(2)
                .withPromptReference("")
                .withPromptSequence(BigDecimal.TEN)
                .withUsergroups(Arrays.asList("GROUP2", "GROUP4"))
                .withType("BOOLEAN")
                .withValue("10£")
                .withQualifier("T")
                .withWelshLabel("WS")
                .withDurationElement(durationElement);
        judicialResultPrompts.add(builder2.build());

        final JudicialResultPrompt.Builder builder4 = judicialResultPrompt();
        builder4.withCourtExtract("Y")
                .withLabel("Boolean Test")
                .withDurationSequence(2)
                .withPromptReference("")
                .withPromptSequence(BigDecimal.TEN)
                .withUsergroups(Arrays.asList("GROUP2", "GROUP4"))
                .withType("BOOLEAN")
                .withValue("No")
                .withQualifier("BT")
                .withWelshLabel("WS")
                .withDurationElement(durationElement);
        judicialResultPrompts.add(builder4.build());

        final JudicialResultPrompt.Builder builder5 = judicialResultPrompt();
        builder4.withCourtExtract("Y")
                .withLabel("Boolean Test")
                .withDurationSequence(2)
                .withPromptReference("")
                .withPromptSequence(BigDecimal.TEN)
                .withUsergroups(Arrays.asList("GROUP2", "GROUP4"))
                .withType("BOOLEAN")
                .withValue("true")
                .withQualifier("FH")
                .withWelshLabel("WS")
                .withDurationElement(durationElement);
        judicialResultPrompts.add(builder5.build());

        return judicialResultPrompts;
    }


    public static IndividualDefendant buildIndividualDefendant() {
        return individualDefendant()
                .withBailConditions("conditions")
                .withBailStatus(BailStatus.bailStatus().withCode("C").withDescription("desc").withId(randomUUID()).build())
                .withPerson(buildIndividualPerson())
                .withPresentAtHearing("T")
                .withReasonForBailConditionsOrCustody("reason")
                .build();
    }

    public static IndividualDefendant buildIndividualDefendantV1() {
        return individualDefendant()
                .withBailConditions("Other: Other test1;Passport - give to the court any passport held;Passport - give to HM Revenue and Customs any passport held;Passport - give to oxford police station any passport held;Passport - surrender passport to oxford police station during football control period;Passport - give to the court any identity card held which could be used as a travel document within the European Economic Area;Passport - give to HM Revenue and Customs any identity card held which could be used as a travel document within the European Economic Area;Passports - give to oxford police station any identity card held which could be used as a travel document within the European Economic Area;Passport - surrender identity card to Oxford police station during football control period;Surety - find a surety in the sum of £100.00;Surety - find a surety in the sum of £500.00 (continuous);Surety - find 2 sureties in the sum of £500.00 each;Security - lodge Bike with Court;Security - pay a deposit of £1000.00 to the Court;Other - give the court a telephone number by 20/04/2023 and tell the court immediately if the number changes or you can no longer be contacted on that number;Assessments/Reports - participate in any assistance or treatment Doctor considers appropriate for misuse of class A drugs;Assessments/Reports - provide pre-sentence drug test samples by 27/04/2023: blood sample;Assessments/Reports - report to the local office of a provider of probation services at 07:00 at oxford on 20/04/2023 for a report to be made;Assessments/Reports - report to oxford a youth justice team at oxford at 07:00 on 20/04/2023 for a report to be made;Computer - not to delete any history of computer use;Curfew - curfew between 07:00 and 19:00 daily;Exclusion - not to sit in the front seat of any motor vehicle;Exclusion - not to leave oxford;Exclusion - not to go more than 1 mile from oxford;Exclusion - not drive a motor vehicle on any road or in any other public place;Exclusion - not go to any public house, licensed club or off licence;Exclusion - not go within 1 mile of radius to oxford;Exclusion - not go within 1 mile of radius unless accompanied by a police officer;Other - to keep mobile phone number 07982291270 switched on, fully charged and on your person 24 hours a day;Other - see solicitor / barrister;Residence - live, and sleep each night, at the test hostel bail hostel;Residence - live, and sleep each night, at the Test hostel bail hostel or at any other place where you may be told to go by Mike;Residence - live and sleep each night at 38, Millway;Residence - not to live in the same household as resident;Residence BASS - take part in additional mandatory support sessions (more than the minimum of one session per week);Surety - find a surety in the sum of £100.00 (continuous);Other - give the court a telephone number by 27/04/2023 and tell the court immediately if the number changes or you can no longer be contacted on that number;Must stay indoors at home address (or at any other address allowed by the court) between 07:00 and 19:00 daily;Must: stay at home;")
                .withBailStatus(BailStatus.bailStatus().withCode("C").withDescription("desc").withId(randomUUID()).build())
                .withPerson(buildIndividualPerson())
                .withPresentAtHearing("T")
                .withReasonForBailConditionsOrCustody("reason")
                .build();
    }

    public static IndividualDefendant buildIndividualDefendantV2() {
        return individualDefendant()
                .withBailConditions("Passport - give to the court any passport held;Passport - give to HM Revenue and Customs any passport held;Passport - give to oxford police station any passport held;Passport - surrender passport to oxford police station during football control period;Passport - give to the court any identity card held which could be used as a travel document within the European Economic Area;")
                .withBailStatus(BailStatus.bailStatus().withCode("C").withDescription("desc").withId(randomUUID()).build())
                .withPerson(buildIndividualPerson())
                .withPresentAtHearing("T")
                .withReasonForBailConditionsOrCustody("reason")
                .build();
    }

    public static IndividualDefendant buildIndividualDefendantV3() {
        return individualDefendant()
                .withBailConditions("Other: Other test1;Passport - give to the court any passport held;Passport - give to HM Revenue and Customs any passport held;Passport - give to oxford police station any passport held;Passport - surrender passport to oxford police station during football control period;Passport - give to the court any identity card held which could be used as a travel document within the European Economic Area;Passport - give to HM Revenue and Customs any identity card held which could be used as a travel document within the European Economic Area;Passports - give to oxford police station any identity card held which could be used as a travel document within the European Economic Area;Passport - surrender identity card to Oxford police station during football control period;Surety - find a surety in the sum of £100.00;Surety - find a surety in the sum of £500.00 (continuous);Surety - find 2 sureties in the sum of £500.00 each;Security - lodge Bike with Court;Security - pay a deposit of £1000.00 to the Court;Other - give the court a telephone number by 20/04/2023 and tell the court immediately if the number changes or you can no longer be contacted on that number;Assessments/Reports - participate in any assistance or treatment Doctor considers appropriate for misuse of class A drugs;Assessments/Reports - provide pre-sentence drug test samples by 27/04/2023: blood sample;Assessments/Reports - report to the local office of a provider of probation services at 07:00 at oxford on 20/04/2023 for a report to be made;Assessments/Reports - report to oxford a youth justice team at oxford at 07:00 on 20/04/2023 for a report to be made;Computer - not to delete any history of computer use;Curfew - curfew between 07:00 and 19:00 daily;Exclusion - not to sit in the front seat of any motor vehicle;Exclusion - not to leave oxford;Exclusion - not to go more than 1 mile from oxford;Exclusion - not drive a motor vehicle on any road or in any other public place;Exclusion - not go to any public house, licensed club or off licence;Exclusion - not go within 1 mile of radius to oxford;Exclusion - not go within 1 mile of radius unless accompanied by a police officer;Other - to keep mobile phone number 07982291270 switched on, fully charged and on your person 24 hours a day;Other - see solicitor / barrister;Residence - live, and sleep each night, at the test hostel bail hostel;Residence - live, and sleep each night, at the Test hostel bail hostel or at any other place where you may be told to go by;abc xyz")
                .withBailStatus(BailStatus.bailStatus().withCode("C").withDescription("desc").withId(randomUUID()).build())
                .withPerson(buildIndividualPerson())
                .withPresentAtHearing("T")
                .withReasonForBailConditionsOrCustody("reason")
                .build();
    }

    public static IndividualDefendant buildIndividualDefendantNoBailConditions() {
        return individualDefendant()
                .withBailStatus(BailStatus.bailStatus().withCode("C").withDescription("desc").withId(randomUUID()).build())
                .withPerson(buildIndividualPerson())
                .withPresentAtHearing("T")
                .withReasonForBailConditionsOrCustody("reason")
                .build();
    }

    public static OrganisationDetails buildCorporateDefendant() {
        return organisationDetails()
                .withAddress(buildPrimaryAddresses())
                .withContact(buildContactNumber())
                .withIncorporationNumber("123456")
                .withName("John Smith")
                .withPresentAtHearing("T")
                .withRegisteredCharityNumber("987654")
                .build();
    }

    public static OrganisationDetails buildCorporateDefendantWithoutContactNumber() {
        return organisationDetails()
                .withAddress(buildPrimaryAddresses())
                .withIncorporationNumber("123456")
                .withName("John Smith")
                .withPresentAtHearing("T")
                .withRegisteredCharityNumber("987654")
                .build();
    }

    public static Individual buildIndividualPerson() {
        return individual()
                .withFirstName("Tina")
                .withLastName("Smith")
                .withTitle(TITLE_MR)
                .withGender(MALE)
                .withDateOfBirth(DATE_OF_BIRTH)
                .withMiddleName("middleName")
                .withAddress(buildPrimaryAddresses())
                .withContact(buildContactNumber())
                .withNationality("UK")
                .build();
    }

    public static List<AttendanceDay> buildListOfAttendanceDays() {
        List<AttendanceDay> attendanceDays = new ArrayList<>();
        attendanceDays.add(attendanceDay().withAttendanceType(AttendanceType.IN_PERSON)
                .withDay(LOCAL_DATE)
                .build());
        return attendanceDays;
    }

    public static ContactNumber buildContactNumber() {
        return contactNumber()
                .withPrimaryEmail("primaryemail@gmail.com")
                .withSecondaryEmail("secondaryemail@gmail.com")
                .withFax("1223456789")
                .withHome("55555555555")
                .withMobile("6666666666")
                .withWork("7777777777")
                .build();
    }

    public static ContactNumber buildContactNumberWithoutTelephoneDetails() {
        return contactNumber()
                .withPrimaryEmail("primaryemail@gmail.com")
                .withSecondaryEmail("secondaryemail@gmail.com")
                .withFax("1223456789")
                .build();
    }

    public static ContactNumber buildContactNumber(final String primaryMail, final String secondaryMail) {
        return contactNumber()
                .withPrimaryEmail(primaryMail)
                .withSecondaryEmail(secondaryMail)
                .withFax("1223456789")
                .withHome("55555555555")
                .withMobile("6666666666")
                .withWork("7777777777")
                .build();
    }

    public static Address buildPrimaryAddresses() {
        return address()
                .withAddress1("101 green house")
                .withAddress2("address2")
                .withAddress3("address3")
                .withAddress4("address4")
                .withPostcode("XE10 FR")
                .build();

    }

    public static List<SessionDay> buildListOfSessionDay() {
        return of(sessionDay().withListedDurationMinutes(10).withListingSequence(10).withSittingDay(SITTING_DAY).build());
    }

    public static CourtCentreWithLJA buildCourtCentreWithLJA() {
        return courtCentreWithLJA()
                .withPsaCode(PSA_CODE)
                .withCourtHearingLocation("HearingLocation")
                .withCourtCentre(buildCourtCentre()).build();
    }

    private static CourtCentre buildCourtCentre() {
        return courtCentre()
                .withAddress(buildPrimaryAddresses())
                .withId(ID)
                .withName("Name")
                .withRoomId(randomUUID())
                .withRoomName("RoomName")
                .withWelshName("WelshName")
                .withWelshRoomName("WelshRoomName")
                .build();
    }

    public static Individual buildPersonIndividual() {
        return individual()
                .withFirstName(FIRST_NAME)
                .withMiddleName(MIDDLE_NAME)
                .withLastName(LAST_NAME)
                .withTitle(TITLE)
                .withDateOfBirth(LOCAL_DATE)
                .withGender(FEMALE)
                .build();
    }

    public static Individual buildPersonIndividual(final ContactNumber contactNumber) {
        return individual()
                .withFirstName(FIRST_NAME)
                .withMiddleName(MIDDLE_NAME)
                .withLastName(LAST_NAME)
                .withTitle(TITLE)
                .withDateOfBirth(LOCAL_DATE)
                .withGender(FEMALE)
                .withContact(contactNumber)
                .build();
    }

}
