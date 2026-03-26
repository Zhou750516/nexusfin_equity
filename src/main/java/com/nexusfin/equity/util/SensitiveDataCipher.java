package com.nexusfin.equity.util;

import com.nexusfin.equity.config.CryptoProperties;
import com.nexusfin.equity.exception.BizException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class SensitiveDataCipher {

    private static final int IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final CryptoProperties cryptoProperties;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey secretKey;

    public SensitiveDataCipher(CryptoProperties cryptoProperties) {
        this.cryptoProperties = cryptoProperties;
        this.secretKey = buildSecretKey(cryptoProperties.getDek().getPlaintextKeyBase64());
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new BizException("SENSITIVE_DATA_EMPTY", "Sensitive data must not be blank");
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(cryptoProperties.getDek().getAlgorithm());
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + encrypted.length)
                    .put(iv)
                    .put(encrypted)
                    .array();
            return cryptoProperties.getDek().getKeyId()
                    + ":" + cryptoProperties.getDek().getKeyVersion()
                    + ":" + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Failed to encrypt sensitive data", exception);
        }
    }

    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isBlank()) {
            throw new BizException("SENSITIVE_DATA_EMPTY", "Sensitive data must not be blank");
        }
        String[] segments = ciphertext.split(":", 3);
        if (segments.length != 3) {
            throw new BizException("SENSITIVE_DATA_FORMAT_INVALID", "Sensitive data format is invalid");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(segments[2]);
            if (payload.length <= IV_LENGTH_BYTES) {
                throw new BizException("SENSITIVE_DATA_FORMAT_INVALID", "Sensitive data payload is invalid");
            }
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] encrypted = new byte[payload.length - IV_LENGTH_BYTES];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(payload, IV_LENGTH_BYTES, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(cryptoProperties.getDek().getAlgorithm());
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (BizException exception) {
            throw exception;
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new BizException("SENSITIVE_DATA_DECRYPT_FAILED", "Failed to decrypt sensitive data");
        }
    }

    public String decodeInbound(String channelCode, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        InboundMode inboundMode = resolveInboundMode(channelCode);
        return switch (inboundMode) {
            case PLAIN -> rawValue;
            case BASE64 -> decodeBase64(rawValue, channelCode);
        };
    }

    private InboundMode resolveInboundMode(String channelCode) {
        String mode = cryptoProperties.getInbound().getChannelModes()
                .getOrDefault(channelCode, cryptoProperties.getInbound().getDefaultMode());
        try {
            return InboundMode.valueOf(mode.trim().toUpperCase());
        } catch (Exception exception) {
            throw new BizException("INBOUND_CRYPTO_MODE_INVALID", "Inbound crypto mode is invalid for channel " + channelCode);
        }
    }

    private String decodeBase64(String rawValue, String channelCode) {
        try {
            return new String(Base64.getDecoder().decode(rawValue), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new BizException("INBOUND_SENSITIVE_DATA_INVALID", "Failed to decode inbound sensitive data for channel " + channelCode);
        }
    }

    private SecretKey buildSecretKey(String keyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(keyBase64);
            if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
                throw new BizException("SENSITIVE_KEY_INVALID", "Sensitive data key length must be 16, 24, or 32 bytes");
            }
            return new SecretKeySpec(keyBytes, "AES");
        } catch (IllegalArgumentException exception) {
            throw new BizException("SENSITIVE_KEY_INVALID", "Sensitive data key must be valid base64");
        }
    }

    private enum InboundMode {
        PLAIN,
        BASE64
    }
}
