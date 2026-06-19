package com.ezboost.util;

public final class UserValidationUtil {

    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]+$";

    private UserValidationUtil() {
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public static String validateProfile(String firstName, String lastName, String username,
                                         String email, String password, String phoneNumber,
                                         boolean passwordRequired) {
        if (isEmpty(firstName) || isEmpty(lastName) || isEmpty(username) ||
                isEmpty(email) || isEmpty(phoneNumber) || (passwordRequired && isEmpty(password))) {
            return "All fields are required. Please fill in every field.";
        }

        if (username.length() < 3) {
            return "Username must be at least 3 characters long.";
        }

        if (!username.matches(USERNAME_PATTERN)) {
            return "Username can only contain letters, numbers, and underscores.";
        }

        if (!email.matches(EMAIL_PATTERN)) {
            return "Please enter a valid email address.";
        }

        if (passwordRequired || !isEmpty(password)) {
            if (password == null || password.length() < 6) {
                return "Password must be at least 6 characters long.";
            }
        }

        String phoneDigits = phoneNumber.replaceAll("[^0-9]", "");
        if (phoneDigits.length() < 7) {
            return "Please enter a valid phone number (at least 7 digits).";
        }

        return null;
    }

    public static boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
