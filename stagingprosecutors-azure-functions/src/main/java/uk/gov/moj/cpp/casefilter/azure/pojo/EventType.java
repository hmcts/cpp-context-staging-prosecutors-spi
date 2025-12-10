package uk.gov.moj.cpp.casefilter.azure.pojo;

public enum EventType {
    CASE_EJECTED("CaseEjected"),
    CASE_ON_CPP("CaseOnCpp");


    private String type;

    EventType(String type) {

        this.type = type;
    }
    public String getType() {

        return type;
    }

}
