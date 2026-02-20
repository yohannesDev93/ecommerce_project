package utils;

import java.util.regex.Pattern;

public class PasswordValidator {
    
    // Simple password requirements
    private static final int MIN_LENGTH = 8;
    
    // Patterns for basic complexity
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?]");
    
    /**
     * Validate password with basic requirements
     */
    public static ValidationResult validatePassword(String password) {
        // Check minimum length
        if (password.length() < MIN_LENGTH) {
            return ValidationResult.failure(
                "Password must be at least " + MIN_LENGTH + " characters"
            );
        }
        
        // Check for uppercase letters
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            return ValidationResult.failure(
                "Password must contain at least one uppercase letter (A-Z)"
            );
        }
        
        // Check for lowercase letters
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            return ValidationResult.failure(
                "Password must contain at least one lowercase letter (a-z)"
            );
        }
        
        // Check for digits (optional if you want numbers)
        if (!DIGIT_PATTERN.matcher(password).find()) {
            return ValidationResult.failure(
                "Password must contain at least one number (0-9)"
            );
        }
        
        // Check for special characters
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            return ValidationResult.failure(
                "Password must contain at least one special character (!@#$% etc.)"
            );
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Get password strength as percentage
     */
    public static int getPasswordStrength(String password) {
        int score = 0;
        
        // Length score (max 40)
        if (password.length() >= 8) score += 20;
        if (password.length() >= 12) score += 20;
        
        // Character type score (max 60)
        if (UPPERCASE_PATTERN.matcher(password).find()) score += 15;
        if (LOWERCASE_PATTERN.matcher(password).find()) score += 15;
        if (DIGIT_PATTERN.matcher(password).find()) score += 15;
        if (SPECIAL_CHAR_PATTERN.matcher(password).find()) score += 15;
        
        return Math.min(score, 100);
    }
    
    /**
     * Get strength category
     */
    public static String getStrengthCategory(String password) {
        int strength = getPasswordStrength(password);
        
        if (strength >= 80) return "STRONG";
        if (strength >= 60) return "GOOD";
        if (strength >= 40) return "FAIR";
        return "WEAK";
    }
    
    /**
     * Check if passwords match
     */
    public static boolean passwordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }
    
    /**
     * Get password requirements description
     */
    public static String getRequirements() {
        return "Password Requirements:\n" +
               "• Minimum 8 characters\n" +
               "• At least one uppercase letter (A-Z)\n" +
               "• At least one lowercase letter (a-z)\n" +
               "• At least one number (0-9)\n" +
               "• At least one special character (!@#$%^&*)";
    }
    
    /**
     * Show example passwords
     */
    public static String[] getExamples() {
        return new String[] {
            "Example1@secure",
            "MyP@ssw0rd123",
            "Secure#2024",
            "Hello@World99"
        };
    }
    
    /**
     * Validation result container
     */
    public static class ValidationResult {
        private final boolean success;
        private final String errorMessage;
        
        private ValidationResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}