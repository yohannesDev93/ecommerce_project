package utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordHasher {
    
    // Use SHA-256 for hashing
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16; // 16 bytes = 128 bits
    
    /**
     * Hash password with random salt
     */
    public static String hashPassword(String password) {
        try {
            // Generate random salt
            SecureRandom random = new SecureRandom();
            byte[] salt = new byte[SALT_LENGTH];
            random.nextBytes(salt);
            
            // Hash password with salt
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedPassword = md.digest(password.getBytes());
            
            // Combine salt + hash
            byte[] combined = new byte[salt.length + hashedPassword.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(hashedPassword, 0, combined, salt.length, hashedPassword.length);
            
            // Return as Base64 string
            return Base64.getEncoder().encodeToString(combined);
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    /**
     * Verify password against stored hash
     */
    public static boolean verifyPassword(String password, String storedHash) {
        try {
            // Decode stored hash
            byte[] combined = Base64.getDecoder().decode(storedHash);
            
            // Extract salt (first SALT_LENGTH bytes)
            byte[] salt = new byte[SALT_LENGTH];
            System.arraycopy(combined, 0, salt, 0, salt.length);
            
            // Extract stored password hash
            byte[] storedPasswordHash = new byte[combined.length - SALT_LENGTH];
            System.arraycopy(combined, SALT_LENGTH, storedPasswordHash, 0, storedPasswordHash.length);
            
            // Hash input password with same salt
            MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
            md.update(salt);
            byte[] hashedInputPassword = md.digest(password.getBytes());
            
            // Compare hashes
            return MessageDigest.isEqual(storedPasswordHash, hashedInputPassword);
            
        } catch (Exception e) {
            System.err.println("Error verifying password: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a string looks like a hashed password
     */
    public static boolean isHashed(String password) {
        try {
            // Hashed passwords are Base64 strings of specific length
            if (password == null || password.length() < 44) { // Minimum Base64 length for salt+hash
                return false;
            }
            
            // Try to decode as Base64
            Base64.getDecoder().decode(password);
            return true;
            
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}