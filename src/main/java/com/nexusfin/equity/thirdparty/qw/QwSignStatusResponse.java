package com.nexusfin.equity.thirdparty.qw;

import com.fasterxml.jackson.annotation.JsonAlias;

public record QwSignStatusResponse(
        @JsonAlias("signStatus")
        Integer status,
        Long userSignId,
        String applyTime
) {

    public QwSignStatusResponse(Integer status) {
        this(status, null, null);
    }
}
