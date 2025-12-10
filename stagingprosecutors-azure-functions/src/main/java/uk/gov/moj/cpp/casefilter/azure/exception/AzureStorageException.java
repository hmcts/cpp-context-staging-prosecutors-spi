package uk.gov.moj.cpp.casefilter.azure.exception;

public class AzureStorageException extends RuntimeException {
    public AzureStorageException(String message, Exception e) {
        super(message, e);
    }
}
