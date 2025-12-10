package uk.gov.moj.cpp.casefilter.azure.utils;

import uk.gov.moj.cpp.casefilter.azure.exception.MissingFieldException;

import java.util.function.Supplier;

public class ExceptionProvider {
    private ExceptionProvider() {
    }
    public static Supplier<MissingFieldException> generateMissingFieldException(final String fieldName) {
        return () -> new MissingFieldException(fieldName);
    }
}
