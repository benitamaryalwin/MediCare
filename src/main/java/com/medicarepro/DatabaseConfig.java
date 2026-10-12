package com.medicarepro;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public record DatabaseConfig(String jdbcUrl, String username, String password) {
    public static DatabaseConfig fromEnvironment() {
        String configuredUrl = firstNonBlank(System.getenv("MEDICARE_DB_URL"),
                "jdbc:postgresql://localhost:5432/medicare_pro");
        String username = firstNonBlank(System.getenv("MEDICARE_DB_USER"),
                System.getenv("PGUSER"), "postgres");
        String password = firstNonBlank(System.getenv("MEDICARE_DB_PASSWORD"),
                System.getenv("PGPASSWORD"), "");
        if (configuredUrl.startsWith("jdbc:postgresql://")) {
            return new DatabaseConfig(configuredUrl, username, password);
        }
        if (configuredUrl.startsWith("postgres://") || configuredUrl.startsWith("postgresql://")) {
            return fromPostgresUri(configuredUrl);
        }
        throw new IllegalArgumentException("MEDICARE_DB_URL must be a PostgreSQL JDBC or postgres:// URL.");
    }

    public Properties properties() {
        Properties properties = new Properties();
        properties.setProperty("user", username);
        properties.setProperty("password", password);
        properties.setProperty("ApplicationName", "MediCare Pro Swing");
        return properties;
    }

    private static DatabaseConfig fromPostgresUri(String raw) {
        URI uri = URI.create(raw);
        if (uri.getHost() == null || uri.getPath() == null || uri.getPath().length() < 2) {
            throw new IllegalArgumentException("MEDICARE_DB_URL must include a PostgreSQL host and database name.");
        }
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        String url = "jdbc:postgresql://" + uri.getHost() + ":" +
                (uri.getPort() > 0 ? uri.getPort() : 5432) + uri.getRawPath() + query;
        String user = System.getenv("MEDICARE_DB_USER");
        String password = System.getenv("MEDICARE_DB_PASSWORD");
        if (uri.getRawUserInfo() != null) {
            String[] credentials = uri.getRawUserInfo().split(":", 2);
            if (user == null || user.isBlank()) user = decode(credentials[0]);
            if ((password == null || password.isBlank()) && credentials.length > 1) {
                password = decode(credentials[1]);
            }
        }
        return new DatabaseConfig(url,
                firstNonBlank(user, "postgres"), password == null ? "" : password);
    }

    private static String decode(String value) {
        return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }
}
