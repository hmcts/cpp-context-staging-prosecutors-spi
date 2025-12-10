package uk.gov.moj.cpp.staging.command.util;

public class InvalidCaseUrnProvided extends RuntimeException {
    public InvalidCaseUrnProvided(final String message) {
        super(message);
    }
}
