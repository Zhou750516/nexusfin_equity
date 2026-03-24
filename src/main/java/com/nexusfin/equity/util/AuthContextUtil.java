package com.nexusfin.equity.util;

import com.nexusfin.equity.exception.BizException;

public final class AuthContextUtil {

    private static final ThreadLocal<AuthPrincipal> PRINCIPAL_HOLDER = new ThreadLocal<>();

    private AuthContextUtil() {
    }

    public static void bind(AuthPrincipal principal) {
        PRINCIPAL_HOLDER.set(principal);
    }

    public static AuthPrincipal getPrincipal() {
        return PRINCIPAL_HOLDER.get();
    }

    public static AuthPrincipal getRequiredPrincipal() {
        AuthPrincipal principal = PRINCIPAL_HOLDER.get();
        if (principal == null) {
            throw new BizException(401, "Unauthorized");
        }
        return principal;
    }

    public static void clear() {
        PRINCIPAL_HOLDER.remove();
    }
}
