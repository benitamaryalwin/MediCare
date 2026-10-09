package com.medicarepro;
import java.nio.charset.StandardCharsets; import java.nio.file.*; import java.sql.*;
public final class Database {
    private final String url="jdbc:sqlite:"+Path.of("medicare.db").toAbsolutePath();
    public Connection connect() throws SQLException { Connection c=DriverManager.getConnection(url); try(Statement s=c.createStatement()){s.execute("PRAGMA foreign_keys=ON");} return c; }
    public void initialize() throws Exception { String sql=Files.readString(Path.of("database","schema.sql"),StandardCharsets.UTF_8); try(Connection c=connect();Statement s=c.createStatement()){for(String q:sql.split(";"))if(!q.isBlank())s.execute(q);} }
}
