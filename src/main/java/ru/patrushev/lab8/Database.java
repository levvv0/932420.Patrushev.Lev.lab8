package ru.patrushev.lab8;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.stereotype.Component;

@Component
public class Database {
    private static final String URL = "jdbc:sqlite:data/equipment.db";

    public Database() {
        try {
            Files.createDirectories(Path.of("data"));
            try (Connection connection = open(); Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS equipment (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL,
                            type TEXT NOT NULL,
                            serial_number TEXT NOT NULL UNIQUE,
                            status TEXT NOT NULL DEFAULT 'available'
                                CHECK (status IN ('available', 'in_use', 'maintenance'))
                        )
                        """);
                statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS loans (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            equipment_id INTEGER NOT NULL REFERENCES equipment(id) ON DELETE CASCADE,
                            borrower TEXT NOT NULL,
                            issue_date TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%fZ', 'now')),
                            return_date TEXT
                        )
                        """);
                statement.executeUpdate("""
                        CREATE UNIQUE INDEX IF NOT EXISTS one_active_loan
                        ON loans(equipment_id) WHERE return_date IS NULL
                        """);
            }
        } catch (Exception error) {
            throw new IllegalStateException("Не удалось открыть базу данных", error);
        }
    }

    public Connection open() throws SQLException {
        Connection connection = DriverManager.getConnection(URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }
}
