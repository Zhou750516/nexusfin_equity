package com.nexusfin.equity.exception;

public final class ErrorCodes {

    public static final String BIZ_ERROR = "BIZ_ERROR";
    public static final String PARAM_INVALID = "PARAM_INVALID";
    public static final String PARAM_TYPE_INVALID = "PARAM_TYPE_INVALID";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String UPSTREAM_REQUEST_FAILED = "UPSTREAM_REQUEST_FAILED";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String YUNKA_UPSTREAM_TIMEOUT = "YUNKA_UPSTREAM_TIMEOUT";
    public static final String YUNKA_UPSTREAM_FAILED = "YUNKA_UPSTREAM_FAILED";
    public static final String YUNKA_RESPONSE_EMPTY = "YUNKA_RESPONSE_EMPTY";
    public static final String YUNKA_UPSTREAM_REJECTED = "YUNKA_UPSTREAM_REJECTED";

    private ErrorCodes() {
    }

    public static String fromHttpStatus(int statusCode) {
        return switch (statusCode) {
            case 400 -> PARAM_INVALID;
            case 401 -> UNAUTHORIZED;
            case 404 -> RESOURCE_NOT_FOUND;
            case 502 -> UPSTREAM_REQUEST_FAILED;
            case 500 -> INTERNAL_SERVER_ERROR;
            default -> BIZ_ERROR;
        };
    }
}
