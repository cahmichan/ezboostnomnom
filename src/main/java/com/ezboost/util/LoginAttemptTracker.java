package com.ezboost.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Lightweight single-server throttling for failed login attempts. */
public final class LoginAttemptTracker {

    private static final int MAX_FAILURES = 5;
    private static final long WINDOW_MILLIS = 15L * 60L * 1000L;
    private static final Map<String, AttemptState> ATTEMPTS = new ConcurrentHashMap<>();

    private LoginAttemptTracker() {
    }

    public static boolean isBlocked(String email, String remoteAddress) {
        AttemptState state = ATTEMPTS.get(key(email, remoteAddress));
        if (state == null) return false;
        synchronized (state) {
            if (expired(state)) {
                ATTEMPTS.remove(key(email, remoteAddress), state);
                return false;
            }
            return state.failures >= MAX_FAILURES;
        }
    }

    public static void recordFailure(String email, String remoteAddress) {
        String key = key(email, remoteAddress);
        AttemptState state = ATTEMPTS.computeIfAbsent(key, ignored -> new AttemptState());
        synchronized (state) {
            if (expired(state)) {
                state.failures = 0;
            }
            state.failures++;
            state.lastFailureAt = System.currentTimeMillis();
        }
    }

    public static void clear(String email, String remoteAddress) {
        ATTEMPTS.remove(key(email, remoteAddress));
    }

    static void clearAllForTests() {
        ATTEMPTS.clear();
    }

    private static boolean expired(AttemptState state) {
        return System.currentTimeMillis() - state.lastFailureAt > WINDOW_MILLIS;
    }

    private static String key(String email, String remoteAddress) {
        return (email == null ? "" : email.toLowerCase()) + "|" + (remoteAddress == null ? "" : remoteAddress);
    }

    private static final class AttemptState {
        private int failures;
        private long lastFailureAt;
    }
}
