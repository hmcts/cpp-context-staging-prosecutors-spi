package uk.gov.moj.cpp.staging.prosecutors.spi.event.converter.results;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static javax.xml.datatype.DatatypeFactory.newInstance;
import static org.apache.commons.lang3.StringUtils.isAnyBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.trim;

import uk.gov.dca.xmlschemas.libra.BaseEmailDetailStructure;
import uk.gov.dca.xmlschemas.libra.BasePersonDetailStructure;
import uk.gov.dca.xmlschemas.libra.BasePersonNameStructure;
import uk.gov.dca.xmlschemas.libra.BaseSimpleAddressStructure;
import uk.gov.dca.xmlschemas.libra.BaseTelephoneDetailStructure;
import uk.gov.dca.xmlschemas.libra.CourtAddressStructure;
import uk.gov.justice.core.courts.Address;
import uk.gov.justice.core.courts.ContactNumber;
import uk.gov.moj.cpp.staging.prosecutors.spi.event.helper.GenderType;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.AttendanceDay;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.AttendanceType;
import uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas.Individual;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonMethods {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommonMethods.class);
    private static final int BEGIN_INDEX = 0;
    private static final int END_INDEX = 35;


    private CommonMethods() {
    }

    public static XMLGregorianCalendar getXmlGregorianCalendarFromLocalDate(final LocalDate localDate) {
        XMLGregorianCalendar xmlGregorianCalendar = null;
        if (null != localDate) {
            try {
                xmlGregorianCalendar = newInstance().newXMLGregorianCalendar(localDate.toString());
            } catch (final DatatypeConfigurationException e) {
                LOGGER.error("unable to convert date - {} {}", localDate, e);
            }
        }
        return xmlGregorianCalendar;
    }

    public static XMLGregorianCalendar getXmlGregorianCalendar(final ZonedDateTime zonedDateTime) {
        XMLGregorianCalendar xmlGregorianCalendar = null;
        if (null != zonedDateTime) {
            try {
                final GregorianCalendar gregorianCalendar = GregorianCalendar.from(zonedDateTime);
                xmlGregorianCalendar = newInstance().newXMLGregorianCalendar(gregorianCalendar);
                xmlGregorianCalendar.setTimezone(DatatypeConstants.FIELD_UNDEFINED);
                xmlGregorianCalendar.setMillisecond(DatatypeConstants.FIELD_UNDEFINED);
            } catch (final DatatypeConfigurationException e) {
                LOGGER.error("unable to convert date - {} {}", zonedDateTime, e);
            }
        }
        return xmlGregorianCalendar;
    }

    public static String subStringToLimit35(final String value) {
        return ofNullable(value)
                .filter(str -> str.length() > 35)
                .map(str -> str.substring(BEGIN_INDEX, END_INDEX))
                .orElse(value);
    }


    public static BasePersonNameStructure buildBasePersonNameStructure(final Individual person) {
        final BasePersonNameStructure basePersonNameStructure = new BasePersonNameStructure();
        basePersonNameStructure.setPersonGivenName1(subStringToLimit35(person.getFirstName()));
        basePersonNameStructure.setPersonGivenName2(subStringToLimit35(person.getMiddleName()));
        basePersonNameStructure.setPersonFamilyName(subStringToLimit35(person.getLastName()));
        basePersonNameStructure.setPersonTitle(null != person.getTitle() ? person.getTitle() : null);
        return basePersonNameStructure;
    }

    public static Optional<BaseEmailDetailStructure> buildBaseEmailDetailsStructure(final ContactNumber contact) {
        if (isNull(contact)) {
            return empty();
        }
        final String primaryEmail = contact.getPrimaryEmail();
        final String secondaryEmail = contact.getSecondaryEmail();
        if (isBlank(primaryEmail) && isBlank(secondaryEmail)) {
            return empty();
        }

        final BaseEmailDetailStructure baseEmailDetailStructure = new BaseEmailDetailStructure();

        if (isBlank(primaryEmail) && isNotBlank(secondaryEmail)) {
            baseEmailDetailStructure.setEmailAddress1(secondaryEmail);
        } else {
            baseEmailDetailStructure.setEmailAddress1(primaryEmail);
            baseEmailDetailStructure.setEmailAddress2(secondaryEmail);
        }
        return ofNullable(baseEmailDetailStructure);
    }

    public static BaseTelephoneDetailStructure buildBaseTelephoneDetailStructure(final ContactNumber contactNumber) {
        if (isNotEmpty(contactNumber.getWork()) || isNotEmpty(contactNumber.getHome()) || isNotEmpty(contactNumber.getMobile())) {
            final BaseTelephoneDetailStructure baseTelephoneDetailStructure = new BaseTelephoneDetailStructure();
            baseTelephoneDetailStructure.setTelephoneNumberBusiness(contactNumber.getWork());
            baseTelephoneDetailStructure.setTelephoneNumberHome(contactNumber.getHome());
            baseTelephoneDetailStructure.setTelephoneNumberMobile(contactNumber.getMobile());
            return baseTelephoneDetailStructure;
        }
        return null;
    }

    public static CourtAddressStructure buildCourtAddressStructure(final Address address) {
        final CourtAddressStructure courtAddressStructure = new CourtAddressStructure();
        final BaseSimpleAddressStructure baseSimpleAddress = new BaseSimpleAddressStructure();
        baseSimpleAddress.setAddressLine1(subStringToLimit35(address.getAddress1()));
        baseSimpleAddress.setAddressLine2(collapseString(subStringToLimit35(address.getAddress2())));
        baseSimpleAddress.setAddressLine3(collapseString(subStringToLimit35(address.getAddress3())));
        baseSimpleAddress.setAddressLine4(collapseString(subStringToLimit35(address.getAddress4())));
        if (nonNull(address.getPostcode())) {
            baseSimpleAddress.setAddressLine5(subStringToLimit35(address.getPostcode()));
        } else {
            baseSimpleAddress.setAddressLine5(subStringToLimit35(address.getAddress5()));
        }
        courtAddressStructure.setSimpleAddress(baseSimpleAddress);
        return courtAddressStructure;
    }

    public static String getPresentAtHearing(final List<AttendanceDay> attendanceDays) {
        String result = "F";
        if (null != attendanceDays) {
            final Optional<AttendanceDay> attendanceDay = attendanceDays.stream().filter(a -> a.getAttendanceType() != AttendanceType.NOT_PRESENT).findFirst();
            if (attendanceDay.isPresent()) {
                result = "T";
            }
        }
        return result;
    }

    public static BasePersonDetailStructure buildBasePersonDetailStructure(final Individual person) {

        final BasePersonDetailStructure personDetails = new BasePersonDetailStructure();
        final Optional<BaseEmailDetailStructure> baseEmailDetailStructure = buildBaseEmailDetailsStructure(person.getContact());
        baseEmailDetailStructure.ifPresent(personDetails::setEmailDetails);

        if (nonNull(person.getContact())) {
            personDetails.setTelephoneDetails(buildBaseTelephoneDetailStructure(person.getContact()));
        }
        if (nonNull(person.getDateOfBirth())) {
            personDetails.setBirthdate(getXmlGregorianCalendarFromLocalDate(person.getDateOfBirth()));
        }

        final int genderInt = GenderType.valueFor(person.getGender().name());
        personDetails.setGender((byte) genderInt);
        personDetails.setPersonName(buildBasePersonNameStructure(person));

        return personDetails;
    }

    private static String collapseString(final String address) {
        return isAnyBlank(address) ? null : trim(address);
    }

}
