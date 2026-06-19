package com.ezboost.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginAttemptTrackerTest {

    @AfterEach
    void clearAttempts() {
        LoginAttemptTracker.clearAllForTests();
    }

    @Test
    void blocksAfterFiveFailuresAndClearsOnSuccess() {
        for (int i = 0; i < 5; i++) {
            LoginAttemptTracker.recordFailure("user@example.com", "127.0.0.1");
        }

        assertTrue(LoginAttemptTracker.isBlocked("user@example.com", "127.0.0.1"));
        LoginAttemptTracker.clear("user@example.com", "127.0.0.1");
        assertFalse(LoginAttemptTracker.isBlocked("user@example.com", "127.0.0.1"));
    }
}
