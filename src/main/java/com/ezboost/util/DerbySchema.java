package com.ezboost.util;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Locale;

/** Resolves the schema Derby assigns to the authenticated application account. */
final class DerbySchema {

    private DerbySchema() {
    }

    static String current(DatabaseMetaData metadata) throws SQLException {
        String userName = metadata.getUserName();
        return userName == null || userName.trim().isEmpty()
                ? "APP" : userName.trim().toUpperCase(Locale.ROOT);
    }
}
