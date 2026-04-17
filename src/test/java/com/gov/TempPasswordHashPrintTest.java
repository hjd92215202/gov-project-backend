package com.gov;

import com.gov.crypto.PasswordCrypto;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Manual password tool for local use.
 *
 * Usage (generate only):
 * mvn -q -Dtest=TempPasswordHashPrintTest -Dpassword.tool.run=true test
 *
 * Usage (generate for custom account/password):
 * mvn -q -Dtest=TempPasswordHashPrintTest -Dpassword.tool.run=true -Dpassword.tool.username=admin -Dpassword.tool.raw=admin123 test
 *
 * Usage (generate and reset DB):
 * mvn -q -Dtest=TempPasswordHashPrintTest -Dpassword.tool.run=true -Dpassword.tool.reset-db=true test
 *
 * Optional DB overrides:
 * -Dpassword.tool.db.url=jdbc:mariadb://localhost:13306/gov_db
 * -Dpassword.tool.db.username=db_user
 * -Dpassword.tool.db.password=Egov@123
 */
public class TempPasswordHashPrintTest {

    @Test
    void generateOrResetPassword() throws Exception {
        Assumptions.assumeTrue(Boolean.parseBoolean(read("password.tool.run", null, "false")),
                "Skip manual password tool. Set -Dpassword.tool.run=true to execute.");

        String username = read("password.tool.username", null, "admin");
        String rawPassword = read("password.tool.raw", null, "admin123");
        String pepper = read("password.tool.pepper", "GOV_PASSWORD_PEPPER", "");

        PasswordCrypto.configurePepper(pepper);
        String encoded = PasswordCrypto.encode(rawPassword, username);

        System.out.println("PASSWORD_TOOL_USERNAME=" + username);
        System.out.println("PASSWORD_TOOL_PEPPER_LENGTH=" + pepper.length());
        System.out.println("PASSWORD_TOOL_BCRYPT=" + encoded);

        boolean resetDb = Boolean.parseBoolean(read("password.tool.reset-db", null, "false"));
        if (!resetDb) {
            System.out.println("PASSWORD_TOOL_DB_RESET=SKIPPED");
            return;
        }

        String dbUrl = read("password.tool.db.url", "GOV_DB_URL", null);
        String dbUsername = read("password.tool.db.username", "GOV_DB_USERNAME", null);
        String dbPassword = read("password.tool.db.password", "GOV_DB_PASSWORD", null);

        if (isBlank(dbUrl) || isBlank(dbUsername)) {
            throw new IllegalStateException("Missing DB config. Provide GOV_DB_URL/GOV_DB_USERNAME or -Dpassword.tool.db.*");
        }

        try (Connection connection = DriverManager.getConnection(dbUrl, dbUsername, valueOrEmpty(dbPassword))) {
            int updated;
            try (PreparedStatement updateStmt = connection.prepareStatement(
                    "UPDATE sys_user SET password = ? WHERE username = ?")) {
                updateStmt.setString(1, encoded);
                updateStmt.setString(2, username);
                updated = updateStmt.executeUpdate();
            }
            if (updated != 1) {
                throw new IllegalStateException("Expected to update 1 row, but updated " + updated + " rows for username=" + username);
            }

            try (PreparedStatement verifyStmt = connection.prepareStatement(
                    "SELECT LENGTH(password) FROM sys_user WHERE username = ?")) {
                verifyStmt.setString(1, username);
                try (ResultSet resultSet = verifyStmt.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new IllegalStateException("Verify failed. User not found after update: " + username);
                    }
                    System.out.println("PASSWORD_TOOL_DB_RESET=OK");
                    System.out.println("PASSWORD_TOOL_DB_PASSWORD_LEN=" + resultSet.getInt(1));
                }
            }
        }
    }

    private static String read(String propertyKey, String envKey, String defaultValue) {
        String propertyValue = System.getProperty(propertyKey);
        if (!isBlank(propertyValue)) {
            return propertyValue.trim();
        }
        if (!isBlank(envKey)) {
            String envValue = System.getenv(envKey);
            if (!isBlank(envValue)) {
                return envValue.trim();
            }
        }
        return defaultValue;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
