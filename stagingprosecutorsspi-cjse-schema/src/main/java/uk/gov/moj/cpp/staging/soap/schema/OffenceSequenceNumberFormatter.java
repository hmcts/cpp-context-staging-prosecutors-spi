package uk.gov.moj.cpp.staging.soap.schema;

public class OffenceSequenceNumberFormatter {


    private OffenceSequenceNumberFormatter() {
    }

    public static String printInt(Integer value) {
        return String.format("%03d", value);
    }

    public static Integer parseInt(String value) {
        return Integer.valueOf(value);
    }

}