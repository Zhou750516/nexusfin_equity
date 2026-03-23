package com.nexusfin.equity.config;

import com.nexusfin.equity.exception.BizException;
import com.nexusfin.equity.util.SignatureUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class SignatureInterceptor implements HandlerInterceptor {

    public static final String HEADER_APP_ID = "X-App-Id";
    public static final String HEADER_TIMESTAMP = "X-Timestamp";
    public static final String HEADER_NONCE = "X-Nonce";
    public static final String HEADER_SIGNATURE = "X-Signature";

    private final SignatureProperties signatureProperties;

    public SignatureInterceptor(SignatureProperties signatureProperties) {
        this.signatureProperties = signatureProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestUri = request.getRequestURI();
        // 仅对外部接入和回调入口做签名校验，避免把内部健康检查也纳入校验范围。
        boolean protectedPath = signatureProperties.getProtectedPathPrefixes().stream()
                .anyMatch(requestUri::startsWith);
        if (!protectedPath) {
            return true;
        }
        String appId = request.getHeader(HEADER_APP_ID);
        String timestamp = request.getHeader(HEADER_TIMESTAMP);
        String nonce = request.getHeader(HEADER_NONCE);
        String signature = request.getHeader(HEADER_SIGNATURE);
        if (appId == null || timestamp == null || nonce == null || signature == null) {
            throw new BizException("SIGNATURE_MISSING", "Missing signature headers");
        }
        if (!signatureProperties.getAppId().equals(appId)) {
            throw new BizException("SIGNATURE_INVALID", "Invalid app id");
        }
        long requestTimestamp = Long.parseLong(timestamp);
        long skewSeconds = Math.abs(Instant.now().getEpochSecond() - requestTimestamp);
        if (skewSeconds > signatureProperties.getMaxSkewSeconds()) {
            throw new BizException("SIGNATURE_EXPIRED", "Signature timestamp expired");
        }
        // 这里先用“应用标识 + 时间戳 + 随机串”做基础签名，后续如果合作方协议要求，也可以扩展为 body 参与签名。
        String expected = SignatureUtil.sign(appId, timestamp, nonce, signatureProperties.getSecret());
        if (!expected.equals(signature)) {
            throw new BizException("SIGNATURE_INVALID", "Invalid signature");
        }
        return true;
    }
}
