package com.ezboost.util;

/** Validates text that is persisted and later rendered in HTML or JavaScript views. */
public final class DisplayTextValidator {

    private DisplayTextValidator() {
    }

    public static boolean isSafePlainText(String value, int maximumLength) {
        if (value == null || value.isEmpty() || value.length() > maximumLength) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current < 0x20 || current == '<' || current == '>' || current == '\'' || current == '"') {
                return false;
            }
        }
        return true;
    }

    public static boolean isSafeUploadName(String value) {
        return isSafePlainText(value, 255) && !value.contains("/") && !value.contains("\\");
    }
}
