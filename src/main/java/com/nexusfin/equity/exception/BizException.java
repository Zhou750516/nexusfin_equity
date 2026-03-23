package com.nexusfin.equity.exception;

public class BizException extends RuntimeException {

    private final int code;

    public BizException(String code, String message) {
        this(-1, code + ":" + message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
