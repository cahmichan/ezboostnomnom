package com.ezboost.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayTextValidatorTest {

    @Test
    void acceptsNormalLabelsAndRejectsMarkupDelimiters() {
        assertTrue(DisplayTextValidator.isSafePlainText("Deluxe Room", 100));
        assertFalse(DisplayTextValidator.isSafePlainText("<script>", 100));
        assertFalse(DisplayTextValidator.isSafePlainText("Room \"A\"", 100));
    }

    @Test
    void rejectsUploadPathsAndUnsafeNames() {
        assertTrue(DisplayTextValidator.isSafeUploadName("rooms.csv"));
        assertFalse(DisplayTextValidator.isSafeUploadName("..\\rooms.csv"));
        assertFalse(DisplayTextValidator.isSafeUploadName("<rooms>.csv"));
    }
}
