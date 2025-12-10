package uk.gov.moj.cpp.staging.prosecutorapi.query.api.accesscontrol;

public final class RuleConstants {

    private static final String GROUP_CJSE = "CJSE";
    private static final String GROUP_SYSTEM_USERS = "System Users";


    private RuleConstants() {
        throw new IllegalAccessError("Utility class");
    }

    public static final String[] getCJSEGroups() {
        return new String[]{GROUP_CJSE};
    }

    public static final String[] getSystemUsersAndCJSEGroups() {
        return new String[]{GROUP_SYSTEM_USERS, GROUP_CJSE};
    }

    public static final String[] getSystemUsers() {
        return new String[]{GROUP_SYSTEM_USERS};
    }
}