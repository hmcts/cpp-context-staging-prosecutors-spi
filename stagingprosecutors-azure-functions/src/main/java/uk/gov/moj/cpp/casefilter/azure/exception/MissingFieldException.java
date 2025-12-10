package uk.gov.moj.cpp.casefilter.azure.exception;

import static java.lang.String.format;

public class MissingFieldException extends RuntimeException {
    public MissingFieldException(String fieldName) {
        super(format("%s field not found ", fieldName));
    }
}
