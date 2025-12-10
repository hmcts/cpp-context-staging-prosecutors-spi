package uk.gov.moj.cpp.staging.prosecutors.spi.json.schemas;

public enum PleaValue {
    UNFIT_TO_PLEAD("UNFIT_TO_PLEAD", 3),

    NO_PLEA("NO_PLEA", 0),

    AUTREFOIS_ACQUIT("AUTREFOIS_ACQUIT", 3),

    CHANGE_TO_NOT_GUILTY("CHANGE_TO_NOT_GUILTY", 2),

    AUTREFOIS_CONVICT("AUTREFOIS_CONVICT", 3),

    NOT_GUILTY("NOT_GUILTY", 2),

    GUILTY("GUILTY", 1),

    CHANGE_TO_GUILTY_AFTER_SWORN_IN("CHANGE_TO_GUILTY_AFTER_SWORN_IN", 1),

    CHANGE_TO_GUILTY_MAGISTRATES_COURT("CHANGE_TO_GUILTY_MAGISTRATES_COURT", 1),

    CHANGE_TO_GUILTY_NO_SWORN_IN("CHANGE_TO_GUILTY_NO_SWORN_IN", 1),

    OPPOSES("OPPOSES", 8),

    MCA_GUILTY("MCA_GUILTY", 6),

    CONSENTS("CONSENTS", 7),

    PARDON("PARDON", 3),

    GUILTY_TO_AN_ALTERNATIVE_OFFENCE_NOT_CHARGED_NAMELY("GUILTY_TO_AN_ALTERNATIVE_OFFENCE_NOT_CHARGED_NAMELY", 0),

    GUILTY_TO_A_LESSER_OFFENCE_NAMELY("GUILTY_TO_A_LESSER_OFFENCE_NAMELY", 0),

    DENIES("DENIES", 8),

    ADMITS("ADMITS", 7);

    private final String value;
    private final Integer code;

    PleaValue(String value, Integer code) {
        this.value = value;
        this.code = code;
    }

    @Override
    public String toString() {
        return value;
    }

    public Integer getCode() {
        return code;
    }
}
