package com.stockmarket.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Password utility class providing secure BCrypt hashing and verification.
 */
public class PasswordUtil {

    /**
     * Hash a plaintext password with BCrypt using default cost 10.
     *
     * @param plainText The plain text password.
     * @return The standard BCrypt hashed string ($2a$10$...).
     */
    public static String hashPassword(String plainText) {
        if (plainText == null || plainText.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        return BCrypt.hashpw(plainText, BCrypt.gensalt(10));
    }

    /**
     * Verify a plaintext password against a stored BCrypt hash.
     *
     * @param plainText The candidate plain text password.
     * @param hashed    The stored BCrypt hash.
     * @return true if credentials match, false otherwise.
     */
    public static boolean verifyPassword(String plainText, String hashed) {
        if (plainText == null || hashed == null || !hashed.startsWith("$2a$")) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainText, hashed);
        } catch (Exception e) {
            return false;
        }
    }
}
