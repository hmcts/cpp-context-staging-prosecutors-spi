package uk.gov.moj.cpp.staging.prosecutors.spi.aggregate;

public enum SPIPoliceCaseStatus {

    EJECTED("EJECTED"),

    LIVE("LIVE");

    private final String value;

    SPIPoliceCaseStatus(String value) {
        this.value = value;
    }
}
