package com.ezboost.dao;

import com.ezboost.model.Room;
import com.ezboost.model.User;
import com.ezboost.util.DBConnection;
import org.apache.derby.drda.NetworkServerControl;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomDataDAORollbackIntegrationTest {

    @Test
    void replaceImportRollsBackWhenTheBatchContainsADuplicateRoom() throws Exception {
        int port = findAvailablePort();
        String databaseName = "room_import_rollback_" + System.nanoTime();
        String originalDerbyHome = System.getProperty("derby.system.home");
        String originalDatabaseUrl = System.getProperty("EZBOOST_DB_URL");
        String originalDatabaseUser = System.getProperty("EZBOOST_DB_USER");
        String originalDatabasePassword = System.getProperty("EZBOOST_DB_PASSWORD");

        System.setProperty("derby.system.home", Files.createTempDirectory("ezboost-derby-").toString());
        System.setProperty("EZBOOST_DB_URL", "jdbc:derby://localhost:" + port + "/" + databaseName + ";create=true");
        System.setProperty("EZBOOST_DB_USER", "app");
        System.setProperty("EZBOOST_DB_PASSWORD", "test-password");

        NetworkServerControl derby = new NetworkServerControl(InetAddress.getLoopbackAddress(), port);
        try {
            derby.start(null);
            waitForServer(derby);
            User user = new User("Rollback", "Tester", "rollback-tester", "rollback@example.test",
                    "StrongPassword123!", "0123456789");
            org.junit.jupiter.api.Assertions.assertTrue(UserDAO.registerUser(user));

            Room original = new Room("Original", 200.0, 180.0, 240.0, 75.0, 10);
            assertEquals(1, RoomDataDAO.saveRoomData(user.getUserId(), Collections.singletonList(original), true));

            Room duplicateA = new Room("Duplicate", 200.0, 180.0, 240.0, 75.0, 10);
            Room duplicateB = new Room("Duplicate", 210.0, 190.0, 250.0, 75.0, 10);
            assertThrows(RuntimeException.class, () -> RoomDataDAO.saveRoomData(user.getUserId(),
                    Arrays.asList(duplicateA, duplicateB), true));

            java.util.List<Room> persisted = RoomDataDAO.getAllRooms(user.getUserId());
            assertEquals(1, persisted.size());
            assertEquals("Original", persisted.get(0).getName());
        } finally {
            DBConnection.close();
            try {
                derby.shutdown();
            } finally {
                restoreProperty("derby.system.home", originalDerbyHome);
                restoreProperty("EZBOOST_DB_URL", originalDatabaseUrl);
                restoreProperty("EZBOOST_DB_USER", originalDatabaseUser);
                restoreProperty("EZBOOST_DB_PASSWORD", originalDatabasePassword);
            }
        }
    }

    private void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private int findAvailablePort() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private void waitForServer(NetworkServerControl derby) throws Exception {
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                derby.ping();
                return;
            } catch (Exception ignored) {
                Thread.sleep(100L);
            }
        }
        throw new IllegalStateException("Temporary Derby server did not start");
    }
}
