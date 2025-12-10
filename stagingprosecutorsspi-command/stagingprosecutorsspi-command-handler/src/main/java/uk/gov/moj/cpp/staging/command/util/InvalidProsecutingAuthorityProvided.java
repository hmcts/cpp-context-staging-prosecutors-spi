package uk.gov.moj.cpp.staging.command.util;

public class InvalidProsecutingAuthorityProvided extends RuntimeException {
    public InvalidProsecutingAuthorityProvided(final String message) {
        super(message);
    }
}
