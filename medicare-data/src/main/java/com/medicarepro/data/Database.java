package com.medicarepro.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private final DatabaseConfig config;

    public Database(DatabaseConfig config) {
        this.config = config;
    }

    public Connection connect() throws SQLException {
        return DriverManager.getConnection(config.jdbcUrl(), config.properties());
    }

    public void initializeSchema() throws SQLException, IOException {
        try (InputStream input = Database.class.getResourceAsStream("/database/schema.sql")) {
            if (input == null) throw new IOException("The database schema resource is missing from the application.");
            String[] statements = new String(input.readAllBytes(), StandardCharsets.UTF_8).split(";");
            try (Connection connection = connect(); Statement statement = connection.createStatement()) {
                for (String sql : statements) {
                    if (!sql.isBlank()) statement.execute(sql);
                }
            }
        }
    }

    public void checkConnection() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1");
        }
    }
}
