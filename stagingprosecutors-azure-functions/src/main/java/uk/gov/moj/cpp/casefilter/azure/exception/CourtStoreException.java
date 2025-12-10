package uk.gov.moj.cpp.casefilter.azure.exception;

public class CourtStoreException extends RuntimeException {
    public CourtStoreException() {
        super("Error sending message to courtstore");
    }

    public CourtStoreException(String errorMessage){
        super (errorMessage);
    }

    public CourtStoreException(String errorMessage, Throwable error){
        super (errorMessage, error);
    }
}
