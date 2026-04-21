package com.nexusfin.equity.exception;

public class BizException extends RuntimeException {

    private final int code;
    private final String errorNo;
    private final String errorMsg;

    public BizException(String errorNo, String errorMsg) {
        this(-1, errorNo, errorMsg);
    }

    public BizException(int code, String message) {
        this(code, ErrorCodes.fromHttpStatus(code), message);
    }

    public BizException(int code, String errorNo, String errorMsg) {
        super(formatMessage(errorNo, errorMsg));
        this.code = code;
        this.errorNo = errorNo;
        this.errorMsg = errorMsg;
    }

    public int getCode() {
        return code;
    }

    public String getErrorNo() {
        return errorNo;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    private static String formatMessage(String errorNo, String errorMsg) {
        if (errorNo == null || errorNo.isBlank()) {
            return errorMsg;
        }
        return errorNo + ": " + errorMsg;
    }
}
