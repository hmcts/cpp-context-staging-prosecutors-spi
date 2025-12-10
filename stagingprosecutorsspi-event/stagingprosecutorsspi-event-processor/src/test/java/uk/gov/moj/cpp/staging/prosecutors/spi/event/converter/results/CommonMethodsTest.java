package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.core.courts.Address.address;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.buildBaseEmailDetailsStructure;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.buildBasePersonDetailStructure;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.buildBasePersonNameStructure;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.buildBaseTelephoneDetailStructure;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.buildCourtAddressStructure;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.getPresentAtHearing;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results.CommonMethods.subStringToLimit35;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.buildContactNumber;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.buildContactNumberWithoutTelephoneDetails;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.buildPersonIndividual;
import static uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.TestTemplate.getXmlGregorianCalendar;
import static uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.AttendanceDay.attendanceDay;

import uk.gov.dca.xmlschemas.libra.BaseEmailDetailStructure;
import uk.gov.dca.xmlschemas.libra.BasePersonDetailStructure;
import uk.gov.dca.xmlschemas.libra.BasePersonNameStructure;
import uk.gov.dca.xmlschemas.libra.BaseTelephoneDetailStructure;
import uk.gov.dca.xmlschemas.libra.CourtAddressStructure;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.AttendanceDay;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.AttendanceType;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Individual;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Test;

public class CommonMethodsTest {

    private static final LocalDate LOCAL_DATE = LocalDate.of(2019, 2, 2);
    private static final ZonedDateTime ZONED_DATE_TIME = ZonedDateTime.of(LocalDate.of(2019, 2, 2), LocalTime.of(12, 3, 10), ZoneId.systemDefault());
    private static final ZonedDateTime ZONED_DATE_TIME_WITHOUT_SECONDS = ZonedDateTime.of(LocalDate.of(2019, 2, 2), LocalTime.of(12, 3), ZoneId.systemDefault());
    private static final String STRING_LIMIT_35_TEXT = "testifthestringvalueislessthan35";

    @Test
    public void shouldGetXmlGregorianCalendarFromLocalDate() {
        XMLGregorianCalendar result = CommonMethods.getXmlGregorianCalendarFromLocalDate(LOCAL_DATE);
        assertNotNull(result);
        assertThat(result, is(getXmlGregorianCalendar(LOCAL_DATE)));
    }

    @Test
    public void shouldGetXmlGregorianCalendarWithSeconds() {
        XMLGregorianCalendar result = CommonMethods.getXmlGregorianCalendar(ZONED_DATE_TIME);
        assertNotNull(result);
        assertThat(result.toString(), is("2019-02-02T12:03:10"));
    }

    @Test
    public void shouldGetXmlGregorianCalendarWithNull() {
        XMLGregorianCalendar result = CommonMethods.getXmlGregorianCalendar(null);
        assertNull(result);
    }

    @Test
    public void shouldGetXmlGregorianCalendarWithoutSeconds() {
        XMLGregorianCalendar result = CommonMethods.getXmlGregorianCalendar(ZONED_DATE_TIME_WITHOUT_SECONDS);
        assertNotNull(result);
        assertThat(result.toString(), is("2019-02-02T12:03:00"));
    }

    @Test
    public void shouldSubStringToLimit35StringValueIsGreaterThan35ThenReturnTruncatedValue() {
        String result = subStringToLimit35("testingsubstringfunctionalityifthestringcontentismorethan35chars");
        assertThat(result, is("testingsubstringfunctionalityifthes"));
    }

    @Test
    public void shouldSubStringToLimit35_StringValueIsLessThan35_ThenReturnActualValue() {
        String result = subStringToLimit35(STRING_LIMIT_35_TEXT);
        assertThat(result, is(STRING_LIMIT_35_TEXT));
    }

    @Test
    public void shouldBuildBasePersonNameStructure() {
        BasePersonNameStructure basePersonNameStructure = buildBasePersonNameStructure(buildPersonIndividual());
        Individual individual = buildPersonIndividual();
        assertBasePersonNameStructure(basePersonNameStructure, individual);
    }

    @Test
    public void shouldBuildBaseEmailDetailsStructure() {
        Optional<BaseEmailDetailStructure> baseEmailDetailStructure = buildBaseEmailDetailsStructure(buildContactNumber());

        assertThat(baseEmailDetailStructure.isPresent(), is(true));
        assertThat(baseEmailDetailStructure.get().getEmailAddress1(), is(buildContactNumber().getPrimaryEmail()));
        assertThat(baseEmailDetailStructure.get().getEmailAddress2(), is(buildContactNumber().getSecondaryEmail()));
    }

    @Test
    public void shouldBuildBaseEmailDetailsStructureWithPrimaryAsSecondaryEmail() {
        final ContactNumber contactNumber = buildContactNumber(null, "secondaryemail@gmail.com");

        final Optional<BaseEmailDetailStructure> baseEmailDetailStructure = buildBaseEmailDetailsStructure(contactNumber);

        assertThat(baseEmailDetailStructure.isPresent(), is(true));
        assertThat(baseEmailDetailStructure.get().getEmailAddress1(), is(contactNumber.getSecondaryEmail()));
        assertThat(baseEmailDetailStructure.get().getEmailAddress2(), nullValue());
    }

    @Test
    public void shouldBuildBaseEmailDetailsStructureAsPrimaryEmail() {
        final ContactNumber contactNumber = buildContactNumber("primaryemail@gmail.com", null);

        final Optional<BaseEmailDetailStructure> baseEmailDetailStructure = buildBaseEmailDetailsStructure(contactNumber);

        assertThat(baseEmailDetailStructure.isPresent(), is(true));
        assertThat(baseEmailDetailStructure.get().getEmailAddress1(), is(contactNumber.getPrimaryEmail()));
        assertThat(baseEmailDetailStructure.get().getEmailAddress2(), nullValue());
    }


    @Test
    public void shouldReturnNullBuildBaseEmailDetailsStructureWithNoMail() {
        final ContactNumber contactNumber = buildContactNumber(null, null);

        Optional<BaseEmailDetailStructure> baseEmailDetailStructure = buildBaseEmailDetailsStructure(contactNumber);
        assertThat(baseEmailDetailStructure.isPresent(), is(false));
    }

    @Test
    public void shouldBuildBaseTelephoneDetailStructure() {
        BaseTelephoneDetailStructure baseTelephoneDetailStructure = buildBaseTelephoneDetailStructure(buildContactNumber());

        assertThat(baseTelephoneDetailStructure.getTelephoneNumberHome(), is(buildContactNumber().getHome()));
        assertThat(baseTelephoneDetailStructure.getTelephoneNumberMobile(), is(buildContactNumber().getMobile()));
        assertThat(baseTelephoneDetailStructure.getTelephoneNumberBusiness(), is(buildContactNumber().getWork()));
    }

    @Test
    public void shouldNotBuildBaseTelephoneDetailIfTelephoneDetailsNotPresent() {
        BaseTelephoneDetailStructure baseTelephoneDetailStructure = buildBaseTelephoneDetailStructure(buildContactNumberWithoutTelephoneDetails());
        assertNull(baseTelephoneDetailStructure);
    }

    @Test
    public void shouldBuildCourtAddressStructure() {
        CourtAddressStructure courtAddressStructure = buildCourtAddressStructure(address().build());
        Address primaryAddress = address().build();
        assertAddress(courtAddressStructure, primaryAddress);
    }

    @Test
    public void shouldBuildCourtAddressStructureForUKAddress() {
        final Address address = address().
                withAddress1("address1")
                .withAddress2("address1")
                .withAddress3("address3")
                .withAddress4("address4")
                .withPostcode("SX1 2MY")
                .build();
        CourtAddressStructure courtAddressStructure = buildCourtAddressStructure(address);
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine1(), is(address.getAddress1()));
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine2(), is(address.getAddress2()));
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine3(), is(address.getAddress3()));
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine4(), is(address.getAddress4()));
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine5(), is(address.getPostcode()));
    }

    @Test
    public void shouldBuildCourtAddressStructureForForeignAddress() {
        final Address address = address().
                withAddress1("address1")
                .withAddress2("address1")
                .withAddress3("address3")
                .withAddress4("address4")
                .withAddress5("address5")
                .build();
        CourtAddressStructure courtAddressStructure = buildCourtAddressStructure(address);
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine1(), is(address.getAddress1()));
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine2(), is(address.getAddress2()));
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine3(), is(address.getAddress3()));
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine4(), is(address.getAddress4()));
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine5(), is(address.getAddress5()));
    }

    @Test
    public void shouldBuildCourtAddressStructureWithCollapseForWhiteSpaces() {
        final Address address = address().
                withAddress1("address 1")
                .withAddress2("   ")
                .withAddress3(" street  one")
                .withAddress4(" south   ")
                .withAddress5("")
                .withPostcode("CR20DF")
                .build();
        CourtAddressStructure courtAddressStructure = buildCourtAddressStructure(address);

        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine1(), is("address 1"));
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine2(), nullValue());
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine3(), is("street  one"));
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine4(), is("south"));
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine5(), is("CR20DF"));
    }

    @Test
    public void shouldGetPresentAtHearingIfIsInAttendanceTrue() {
        final Boolean isInAttendance = Boolean.TRUE;
        String result = getPresentAtHearing(buildListOfAttendanceDay(isInAttendance));
        assertThat(result, is("T"));
    }

    @Test
    public void shouldGetPresentAtHearingIfIsInAttendanceFalse() {
        final Boolean isInAttendance = Boolean.FALSE;
        String result = getPresentAtHearing(buildListOfAttendanceDay(isInAttendance));
        assertThat(result, is("F"));
    }

    @Test
    public void shouldBuildBasePersonDetailStructureWithSecondaryEmail() {
        final ContactNumber contactNumber = buildContactNumber(null, "secondaryemail@gmail.com");
        Individual individual = buildPersonIndividual(contactNumber);

        final BasePersonDetailStructure basePersonDetailStructure = buildBasePersonDetailStructure(individual);

        assertThat(basePersonDetailStructure.getEmailDetails().getEmailAddress1(), is(contactNumber.getSecondaryEmail()));
    }


    @Test
    public void shouldBuildBasePersonDetailStructureWithPrimaryEmail() {
        final ContactNumber contactNumber = buildContactNumber("primaryemail@gmail.com", null);
        Individual individual = buildPersonIndividual(contactNumber);

        final BasePersonDetailStructure basePersonDetailStructure = buildBasePersonDetailStructure(individual);

        assertThat(basePersonDetailStructure.getEmailDetails().getEmailAddress1(), is(contactNumber.getPrimaryEmail()));
    }


    @Test
    public void shouldBuildBasePersonDetailStructureWithNoEmail() {
        final ContactNumber contactNumber = buildContactNumber(null, null);
        Individual individual = buildPersonIndividual(contactNumber);

        final BasePersonDetailStructure basePersonDetailStructure = buildBasePersonDetailStructure(individual);

        assertThat(basePersonDetailStructure.getEmailDetails(), nullValue());
    }

    @Test
    public void shouldBuildBasePersonDetailStructure() {

        ContactNumber contactNumber = buildContactNumber();
        Individual individual = buildPersonIndividual();

        final BaseTelephoneDetailStructure baseTelephoneDetailStructure = getBaseTelephoneDetailStructure(contactNumber);
        final BasePersonNameStructure basePersonNameStructure = getBasePersonNameStructure(individual);
        final BaseEmailDetailStructure baseEmailDetailsStructure = getBaseEmailDetailStructure(contactNumber);

        final BasePersonDetailStructure basePersonDetailStructure = buildBasePersonDetailStructure(individual);
        basePersonDetailStructure.setPersonName(basePersonNameStructure);
        basePersonDetailStructure.setTelephoneDetails(baseTelephoneDetailStructure);
        basePersonDetailStructure.setEmailDetails(baseEmailDetailsStructure);
        basePersonDetailStructure.setBirthdate(getXmlGregorianCalendar(individual.getDateOfBirth()));
        basePersonDetailStructure.setGender(Byte.MAX_VALUE); //Need to check

        assertPersonName(basePersonDetailStructure, basePersonNameStructure);
        assertTelephoneDetails(basePersonDetailStructure, baseTelephoneDetailStructure);
        assertEmailDetails(basePersonDetailStructure, baseEmailDetailsStructure);

        assertThat(basePersonDetailStructure.getBirthdate(), is(getXmlGregorianCalendar(individual.getDateOfBirth())));
        assertThat(basePersonDetailStructure.getGender(), is(Byte.MAX_VALUE));

    }

    private BaseEmailDetailStructure getBaseEmailDetailStructure(final ContactNumber contactNumber) {
        final BaseEmailDetailStructure baseEmailDetailsStructure = new BaseEmailDetailStructure();
        baseEmailDetailsStructure.setEmailAddress1(contactNumber.getPrimaryEmail());
        baseEmailDetailsStructure.setEmailAddress2(contactNumber.getSecondaryEmail());
        return baseEmailDetailsStructure;
    }

    private BasePersonNameStructure getBasePersonNameStructure(final Individual individual) {
        final BasePersonNameStructure basePersonNameStructure = new BasePersonNameStructure();
        basePersonNameStructure.setPersonGivenName1(individual.getFirstName());
        basePersonNameStructure.setPersonGivenName2(individual.getMiddleName());
        basePersonNameStructure.setPersonFamilyName(individual.getLastName());
        basePersonNameStructure.setPersonTitle(individual.getTitle().toString());
        return basePersonNameStructure;
    }

    private BaseTelephoneDetailStructure getBaseTelephoneDetailStructure(final ContactNumber contactNumber) {
        final BaseTelephoneDetailStructure baseTelephoneDetailStructure = new BaseTelephoneDetailStructure();
        baseTelephoneDetailStructure.setTelephoneNumberHome(contactNumber.getHome());
        baseTelephoneDetailStructure.setTelephoneNumberMobile(contactNumber.getMobile());
        baseTelephoneDetailStructure.setTelephoneNumberBusiness(contactNumber.getWork());
        return baseTelephoneDetailStructure;
    }

    private void assertEmailDetails(final BasePersonDetailStructure basePersonDetailStructure, final BaseEmailDetailStructure baseEmailDetailsStructure) {
        assertThat(basePersonDetailStructure.getEmailDetails().getEmailAddress1(), is(baseEmailDetailsStructure.getEmailAddress1()));
        assertThat(basePersonDetailStructure.getEmailDetails().getEmailAddress2(), is(baseEmailDetailsStructure.getEmailAddress2()));
    }

    private void assertTelephoneDetails(final BasePersonDetailStructure basePersonDetailStructure, final BaseTelephoneDetailStructure baseTelephoneDetailStructure) {
        assertThat(basePersonDetailStructure.getTelephoneDetails().getTelephoneNumberHome(), is(baseTelephoneDetailStructure.getTelephoneNumberHome()));
        assertThat(basePersonDetailStructure.getTelephoneDetails().getTelephoneNumberMobile(), is(baseTelephoneDetailStructure.getTelephoneNumberMobile()));
        assertThat(basePersonDetailStructure.getTelephoneDetails().getTelephoneNumberBusiness(), is(baseTelephoneDetailStructure.getTelephoneNumberBusiness()));
    }

    private void assertPersonName(final BasePersonDetailStructure basePersonDetailStructure, final BasePersonNameStructure basePersonNameStructure) {
        if (null != basePersonDetailStructure.getPersonName()) {
            assertThat(basePersonDetailStructure.getPersonName().getPersonTitle(), is(basePersonNameStructure.getPersonTitle()));
            assertThat(basePersonDetailStructure.getPersonName().getPersonGivenName1(), is(basePersonNameStructure.getPersonGivenName1()));
            assertThat(basePersonDetailStructure.getPersonName().getPersonGivenName2(), is(basePersonNameStructure.getPersonGivenName2()));
            assertThat(basePersonDetailStructure.getPersonName().getPersonFamilyName(), is(basePersonNameStructure.getPersonFamilyName()));
        }
    }

    private void assertBasePersonNameStructure(final BasePersonNameStructure basePersonNameStructure, Individual individual) {
        assertThat(basePersonNameStructure.getPersonGivenName1(), is(individual.getFirstName()));
        assertThat(basePersonNameStructure.getPersonGivenName2(), is(individual.getMiddleName()));
        assertThat(basePersonNameStructure.getPersonFamilyName(), is(individual.getLastName()));
        assertThat(basePersonNameStructure.getPersonTitle(), is(individual.getTitle().toString()));
    }

    private List<AttendanceDay> buildListOfAttendanceDay(Boolean isInAttendance) {
        List<AttendanceDay> attendanceDays = new ArrayList<>();
        attendanceDays.add(attendanceDay().withAttendanceType(isInAttendance ? AttendanceType.IN_PERSON : AttendanceType.NOT_PRESENT).build());
        return attendanceDays;
    }

    private void assertAddress(final CourtAddressStructure courtAddressStructure, Address primaryAddress) {
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine1(), is(primaryAddress.getAddress1()));
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine2(), is(primaryAddress.getAddress2()));
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine3(), is(primaryAddress.getAddress3()));
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine4(), is(primaryAddress.getAddress4()));
        assertThat(courtAddressStructure.getSimpleAddress().getAddressLine5(), is(primaryAddress.getPostcode()));
    }

}