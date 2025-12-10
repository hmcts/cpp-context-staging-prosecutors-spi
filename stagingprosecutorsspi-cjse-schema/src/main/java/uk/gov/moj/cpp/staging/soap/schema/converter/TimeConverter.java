package uk.gov.moj.cpp.staging.soap.schema.converter;


import uk.gov.justice.services.common.converter.Converter;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.datatype.XMLGregorianCalendar;

public class TimeConverter implements Converter<XMLGregorianCalendar, String> {

    @Override
    public String convert(final XMLGregorianCalendar gregorianCalendar) {
        if (null == gregorianCalendar) {
            return null;
        }
        final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
        final ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(gregorianCalendar.toGregorianCalendar().toInstant(), ZoneId.of("UTC"));
        return zonedDateTime.format(dateTimeFormatter);
    }
}
