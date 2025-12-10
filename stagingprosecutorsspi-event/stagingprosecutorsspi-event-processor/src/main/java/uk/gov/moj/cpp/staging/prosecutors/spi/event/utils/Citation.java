package uk.gov.moj.cpp.staging.prosecutors.spi.event.utils;

public enum Citation {

    STATEMENT_OF_FACTS_WITH_SPACE("-- Statement of Facts"),
    STATEMENT_OF_FACTS_WITH_SPACE_UPPER_CASE("-- STATEMENT OF FACTS"),
    STATEMENT_OF_FACTS_WITH_SPACE_LOWER_CASE("-- statement of facts"),
    STATEMENT_OF_FACTS_WITHOUT_SPACE("--Statement of Facts"),
    STATEMENT_OF_FACTS_WITHOUT_SPACE_UPPER_CASE("--STATEMENT OF FACTS"),
    STATEMENT_OF_FACTS_WITHOUT_SPACE_LOWER_CASE("--statement of facts"),
    LEGISLATION_WITH_SPACE("-- Legislation"),
    LEGISLATION_WITH_SPACE_LOWER_CASE("-- legislation"),
    LEGISLATION_WITH_SPACE_UPPER_CASE("-- LEGISLATION"),
    LEGISLATION_WITHOUT_SPACE("--Legislation"),
    LEGISLATION_WITHOUT_SPACE_LOWER_CASE("--legislation"),
    LEGISLATION_WITHOUT_SPACE_UPPER_CASE("--LEGISLATION");


    private String value;

    Citation(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
