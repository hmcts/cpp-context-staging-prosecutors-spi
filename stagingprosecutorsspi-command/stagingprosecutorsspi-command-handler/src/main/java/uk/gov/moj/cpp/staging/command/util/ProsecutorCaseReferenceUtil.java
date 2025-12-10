package uk.gov.moj.cpp.staging.command.util;

import static java.lang.String.format;

import java.util.Objects;

public class ProsecutorCaseReferenceUtil {

    private static final String PROSECUTOR_CASE_PATTERN = "%s:%s";

    private ProsecutorCaseReferenceUtil() {
    }

    public static String getProsecutorCaseReference(final String prosecutingAuthority, final String caseUrn) {
        if (Objects.isNull(prosecutingAuthority)) {
            throw new InvalidProsecutingAuthorityProvided("please provide a valid prosecutingAuthority");
        }

        if (Objects.isNull(caseUrn)) {
            throw new InvalidCaseUrnProvided("please provide a valid caseUrn");
        }

        return format(PROSECUTOR_CASE_PATTERN, prosecutingAuthority, caseUrn);
    }

}
