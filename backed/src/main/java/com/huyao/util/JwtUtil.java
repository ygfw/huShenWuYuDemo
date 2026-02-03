package com.huyao.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huyao.entity.User;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Component
public class JwtUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private final byte[] secret;
    private final long expirationSeconds;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expirationMinutes:1440}") long expirationMinutes) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = Math.max(1, expirationMinutes) * 60;
    }

    public String generateToken(User user) {
        return generateToken(String.valueOf(user.getId()), user.getUsername());
    }

    public String generateToken(String userId, String username) {
        long now = Instant.now().getEpochSecond();
        long exp = now + expirationSeconds;
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format(
                "{\"sub\":\"%s\",\"username\":\"%s\",\"iat\":%d,\"exp\":%d}",
                userId, escape(username), now, exp);
        String header = ENCODER.encodeToString(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = ENCODER.encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    public JwtUser parseToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("invalid token");
        }
        String header = parts[0];
        String payload = parts[1];
        String signature = parts[2];
        String expected = sign(header + "." + payload);
        if (!constantTimeEquals(signature, expected)) {
            throw new IllegalArgumentException("invalid signature");
        }
        Map<String, Object> data = parsePayload(payload);
        Object expValue = data.get("exp");
        long exp = expValue instanceof Number ? ((Number) expValue).longValue() : 0L;
        if (exp <= Instant.now().getEpochSecond()) {
            throw new IllegalArgumentException("token expired");
        }
        String userId = String.valueOf(data.get("sub"));
        String username = String.valueOf(data.getOrDefault("username", ""));
        return new JwtUser(userId, username);
    }

    private Map<String, Object> parsePayload(String payload) {
        try {
            byte[] decoded = DECODER.decode(payload);
            return MAPPER.readValue(decoded, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("invalid payload");
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] signature = mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
            return ENCODER.encodeToString(signature);
        } catch (Exception ex) {
            throw new IllegalStateException("sign error");
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record JwtUser(String userId, String username) {
    }
}
