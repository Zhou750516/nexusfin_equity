package com.nexusfin.equity.util;

import com.nexusfin.equity.exception.BizException;

public final class ErrorLogFields {

    public static final String UNKNOWN_ERROR = "UNKNOWN_ERROR";
    public static final String UNKNOWN_ERROR_MESSAGE = "Unknown error";
    private static final int MAX_ERROR_MSG_LENGTH = 500;

    private ErrorLogFields() {
    }

    public static String errorNo(Throwable exception, String defaultErrorNo) {
        if (exception instanceof BizException bizException && hasText(bizException.getErrorNo())) {
            return bizException.getErrorNo();
        }
        if (hasText(defaultErrorNo)) {
            return defaultErrorNo;
        }
        if (exception == null) {
            return UNKNOWN_ERROR;
        }
        return exception.getClass().getSimpleName();
    }

    public static String errorMsg(Throwable exception, String defaultErrorMsg) {
        if (exception instanceof BizException bizException && hasText(bizException.getErrorMsg())) {
            return truncate(bizException.getErrorMsg());
        }
        String rootMessage = rootCauseMessage(exception);
        if (hasText(rootMessage)) {
            return truncate(rootMessage);
        }
        if (hasText(defaultErrorMsg)) {
            return truncate(defaultErrorMsg);
        }
        if (exception == null) {
            return UNKNOWN_ERROR_MESSAGE;
        }
        return exception.getClass().getSimpleName();
    }

    private static String rootCauseMessage(Throwable exception) {
        if (exception == null) {
            return null;
        }
        Throwable current = exception;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }

    private static String truncate(String value) {
        if (value == null || value.length() <= MAX_ERROR_MSG_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_MSG_LENGTH);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
