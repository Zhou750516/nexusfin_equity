package com.nexusfin.equity.thirdparty.techplatform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusfin.equity.config.TechPlatformProperties;
import com.nexusfin.equity.exception.BizException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class TechPlatformPayloadCodec {

    private final TechPlatformProperties properties;
    private final ObjectMapper objectMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKey secretKey;

    public TechPlatformPayloadCodec(TechPlatformProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.secretKey = buildSecretKey(properties.getAesKeyBase64());
    }

    public String encrypt(Object businessRequest) {
        try {
            return encrypt(objectMapper.writeValueAsString(businessRequest));
        } catch (JsonProcessingException exception) {
            throw new BizException("TECH_PLATFORM_REQUEST_INVALID", "Failed to serialize tech platform request");
        }
    }

    public String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(properties.getAesAlgorithm());
            if (requiresGcm()) {
                byte[] iv = new byte[properties.getIvLengthBytes()];
                secureRandom.nextBytes(iv);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(properties.getGcmTagBits(), iv));
                byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
                return Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                        .put(iv)
                        .put(encrypted)
                        .array());
            }
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new BizException("TECH_PLATFORM_ENCRYPT_FAILED", "Failed to encrypt tech platform payload");
        }
    }

    public String decrypt(String ciphertext) {
        try {
            byte[] payload = Base64.getDecoder().decode(ciphertext);
            Cipher cipher = Cipher.getInstance(properties.getAesAlgorithm());
            byte[] plaintext;
            if (requiresGcm()) {
                if (payload.length <= properties.getIvLengthBytes()) {
                    throw new BizException("TECH_PLATFORM_RESPONSE_INVALID", "Tech platform encrypted response is invalid");
                }
                byte[] iv = new byte[properties.getIvLengthBytes()];
                byte[] encrypted = new byte[payload.length - properties.getIvLengthBytes()];
                System.arraycopy(payload, 0, iv, 0, iv.length);
                System.arraycopy(payload, iv.length, encrypted, 0, encrypted.length);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(properties.getGcmTagBits(), iv));
                plaintext = cipher.doFinal(encrypted);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, secretKey);
                plaintext = cipher.doFinal(payload);
            }
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (BizException exception) {
            throw exception;
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new BizException("TECH_PLATFORM_DECRYPT_FAILED", "Failed to decrypt tech platform payload");
        }
    }

    public String sign(String timestamp, String param) {
        String payload = timestamp + param;
        try {
            if (properties.getSignAlgorithm() == TechPlatformProperties.SignAlgorithm.MD5) {
                byte[] digest = MessageDigest.getInstance("MD5")
                        .digest((payload + properties.getSignSecret()).getBytes(StandardCharsets.UTF_8));
                return HexFormat.of().formatHex(digest);
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getSignSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new BizException("TECH_PLATFORM_SIGN_FAILED", "Failed to sign tech platform payload");
        }
    }

    private SecretKey buildSecretKey(String keyBase64) {
        try {
            return new SecretKeySpec(Base64.getDecoder().decode(keyBase64), "AES");
        } catch (IllegalArgumentException exception) {
            throw new BizException("TECH_PLATFORM_AES_KEY_INVALID", "Tech platform AES key must be valid base64");
        }
    }

    private boolean requiresGcm() {
        return properties.getAesAlgorithm() != null && properties.getAesAlgorithm().contains("/GCM/");
    }
}
