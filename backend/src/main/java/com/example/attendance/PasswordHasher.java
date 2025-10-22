package com.example.attendance;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final int ITERATIONS = 12000;
    private static final int KEY_LENGTH = 256;

    public HashedPassword hash(String password) {
        Objects.requireNonNull(password, "password");
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt);
        return new HashedPassword(Base64.getEncoder().encodeToString(hash),
                Base64.getEncoder().encodeToString(salt));
    }

    public boolean verify(String password, HashedPassword hashed) {
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(hashed, "hashed");
        byte[] salt = Base64.getDecoder().decode(hashed.salt());
        byte[] expected = Base64.getDecoder().decode(hashed.hash());
        byte[] actual = pbkdf2(password.toCharArray(), salt);
        if (expected.length != actual.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expected.length; i++) {
            result |= expected[i] ^ actual[i];
        }
        return result == 0;
    }

    private byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Unable to hash password", e);
        }
    }

    public record HashedPassword(String hash, String salt) {
    }
}
