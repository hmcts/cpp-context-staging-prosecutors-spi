package uk.gov.moj.cpp.staging.prosecutors.spi.validation;


public enum ValidationError {
    INVALID_SYSTEM_ID_CODE(301, "UnknownSystem"),
    INVALID_PREVIOUSLY_SENT_REQUEST(302, "PreviouslySent"),
    INVALID_SOURCE_ID(304, "WrongSource"),
    INVALID_DESTINATION_ID(305, "WrongDestination"),
    INVALID_CORRELATION_ID_CODE(306, "InvalidCorrelationID"),
    INVALID_REQUEST_ID(306, "InvalidRequestID"),
    INVALID_EXEC_MODE(307, "ModeError"),
    INVALID_DATA_CONTROLLER(307, "InvalidDataController"),
    INVALID_ORGANIZATIONAL_UNIT_ID(308, "InvalidOrganizationalUnitID"),
    INVALID_ROUTE_SOURCE_SYSTEM(1317, "UnknownRouteSourceSystem"),
    INVALID_ROUTE_DESTINATION_SYSTEM(1318, "UnknownRouteDestinationSystem"),
    SAME_SOURCE_AND_DESTINATION_SYSTEM(1321, "SameSourceAndDestinationSystem"),
    DISALLOWED_DATA_STREAM_TYPE(1359, "DisallowedDataStreamType"),
    UNUSABLE_DATA_STREAM(1310, "UnusableDataStream");

    private final int code;
    private final String text;
    private final String errorType;

    ValidationError(final int code, final String text) {
        this.code = code;
        this.text = text;
        this.errorType = "FatalError";
    }

    public int getCode() {
        return code;
    }

    public String getText() {
        return text;
    }

    public String getErrorType() {
        return errorType;
    }
}
