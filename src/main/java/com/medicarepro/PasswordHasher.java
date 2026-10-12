package com.medicarepro;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

public final class PasswordHasher {
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private static final int SALT_BYTES = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    public record PasswordHash(byte[] hash, byte[] salt) { }

    public PasswordHash hash(char[] password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return new PasswordHash(derive(password, salt), salt);
    }

    public boolean verify(char[] password, byte[] expectedHash, byte[] salt) {
        return MessageDigest.isEqual(derive(password, salt), expectedHash);
    }

    private byte[] derive(char[] password, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Secure password hashing is unavailable.", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
