package com.medicarepro;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void acceptsOriginalPasswordAndRejectsDifferentPassword() {
        char[] password = "MediCare-Testing-Only".toCharArray();
        char[] wrongPassword = "not-the-password".toCharArray();
        PasswordHasher.PasswordHash stored = hasher.hash(password);
        try {
            assertTrue(hasher.verify(password, stored.hash(), stored.salt()));
            assertFalse(hasher.verify(wrongPassword, stored.hash(), stored.salt()));
            assertNotEquals(new String(password), new String(stored.hash()));
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(wrongPassword, '\0');
        }
    }

    @Test
    void generatesDifferentSaltForEachHash() {
        char[] password = "same-password".toCharArray();
        try {
            PasswordHasher.PasswordHash first = hasher.hash(password);
            PasswordHasher.PasswordHash second = hasher.hash(password);
            assertFalse(Arrays.equals(first.salt(), second.salt()));
            assertFalse(Arrays.equals(first.hash(), second.hash()));
            assertTrue(hasher.verify(password, first.hash(), first.salt()));
            assertTrue(hasher.verify(password, second.hash(), second.salt()));
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
