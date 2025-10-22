package com.example.attendance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class TokenService {
    private static final String HMAC = "HmacSHA256";
    private final byte[] secret;

    private TokenService(byte[] secret) {
        this.secret = secret.clone();
    }

    public static TokenService load(Path secretFile) {
        try {
            if (Files.exists(secretFile)) {
                byte[] secret = Files.readAllBytes(secretFile);
                return new TokenService(secret);
            }
            byte[] generated = new byte[32];
            new java.security.SecureRandom().nextBytes(generated);
            Files.write(secretFile, generated);
            return new TokenService(generated);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load token secret", e);
        }
    }

    public String generateToken(String username, String role, long validitySeconds) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(role, "role");
        long exp = Instant.now().plusSeconds(validitySeconds).getEpochSecond();
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = String.format("{\"sub\":\"%s\",\"role\":\"%s\",\"exp\":%d}",
                Json.escapeString(username), Json.escapeString(role), exp);
        String header = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    public Optional<TokenPayload> verifyToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }
        String signature = sign(parts[0] + "." + parts[1]);
        if (!constantTimeEquals(signature, parts[2])) {
            return Optional.empty();
        }
        String payloadJson = new String(base64UrlDecode(parts[1]), StandardCharsets.UTF_8);
        Map<String, Object> payload = Json.parseObject(payloadJson);
        String username = (String) payload.get("sub");
        String role = (String) payload.get("role");
        Object expValue = payload.get("exp");
        if (username == null || role == null || expValue == null) {
            return Optional.empty();
        }
        long exp;
        if (expValue instanceof Number number) {
            exp = number.longValue();
        } else {
            exp = Long.parseLong(expValue.toString());
        }
        if (Instant.now().getEpochSecond() >= exp) {
            return Optional.empty();
        }
        return Optional.of(new TokenPayload(username, role));
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret, HMAC));
            return base64UrlEncode(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Unable to sign token", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    public record TokenPayload(String username, String role) {
    }
}
