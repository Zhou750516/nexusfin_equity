package com.nexusfin.equity.dto.response;

import java.io.Serial;
import java.io.Serializable;

public record Result<T>(
        int code,
        String message,
        T data
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static <T> Result<T> success(T data) {
        return new Result<>(0, "OK", data);
    }

    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> failure(String code, String message) {
        return new Result<>(-1, code + ":" + message, null);
    }
}
