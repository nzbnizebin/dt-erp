package com.example.attendance;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Database {
    private final Path databaseFile;

    public Database(Path databaseFile) {
        this.databaseFile = Objects.requireNonNull(databaseFile, "databaseFile");
    }

    public synchronized void initialize() {
        execute("PRAGMA foreign_keys=ON;");
        execute("CREATE TABLE IF NOT EXISTS user_account (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password_hash TEXT NOT NULL, " +
                "salt TEXT NOT NULL, " +
                "role TEXT NOT NULL" +
                ");");
        execute("CREATE TABLE IF NOT EXISTS employee (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "chinese_name TEXT NOT NULL, " +
                "english_name TEXT UNIQUE NOT NULL, " +
                "hire_date TEXT NOT NULL" +
                ");");
        execute("CREATE TABLE IF NOT EXISTS leave_request (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "employee_id INTEGER NOT NULL, " +
                "type TEXT NOT NULL, " +
                "start_time TEXT NOT NULL, " +
                "end_time TEXT NOT NULL, " +
                "hours REAL NOT NULL, " +
                "created_at TEXT NOT NULL, " +
                "FOREIGN KEY(employee_id) REFERENCES employee(id) ON DELETE CASCADE" +
                ");");
        ensureAdminAccount();
    }

    public synchronized void execute(String sql) {
        runProcess(sql, false);
    }

    public synchronized List<Map<String, String>> query(String sql) {
        String output = runProcess(sql, true);
        if (output.isBlank()) {
            return Collections.emptyList();
        }
        List<Map<String, String>> rows = new ArrayList<>();
        String[] lines = output.split("\n");
        if (lines.length == 0) {
            return rows;
        }
        String[] headers = lines[0].split("\u001F", -1);
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isEmpty()) {
                continue;
            }
            String[] values = lines[i].split("\u001F", -1);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.length && j < values.length; j++) {
                row.put(headers[j], values[j]);
            }
            rows.add(row);
        }
        return rows;
    }

    public static String escape(String value) {
        return value.replace("'", "''");
    }

    private String runProcess(String sql, boolean query) {
        List<String> command = new ArrayList<>();
        command.add("sqlite3");
        if (query) {
            command.add("-header");
            command.add("-separator");
            command.add("\u001F");
        }
        command.add(databaseFile.toString());
        command.add("PRAGMA foreign_keys=ON;" + sql);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exit = process.waitFor();
            if (exit != 0) {
                throw new IllegalStateException("SQLite error: " + output.trim());
            }
            return output.trim();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("SQLite execution interrupted", e);
        }
    }

    private void ensureAdminAccount() {
        List<Map<String, String>> existing = query("SELECT id FROM user_account WHERE username='admin';");
        if (!existing.isEmpty()) {
            return;
        }
        PasswordHasher hasher = new PasswordHasher();
        PasswordHasher.HashedPassword hashed = hasher.hash("admin123");
        String sql = String.format("INSERT INTO user_account (username, password_hash, salt, role) " +
                        "VALUES ('admin','%s','%s','ADMIN');",
                escape(hashed.hash()), escape(hashed.salt()));
        execute(sql);
    }
}
