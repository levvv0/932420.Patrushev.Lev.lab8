package ru.patrushev.lab8;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@Repository
public class EquipmentRepository {
    private final Database database;

    public EquipmentRepository(Database database) {
        this.database = database;
    }

    public List<Equipment> findAll(String status, String type) {
        String sql = """
                SELECT * FROM equipment
                WHERE (? IS NULL OR status = ?) AND (? IS NULL OR type = ?)
                ORDER BY name COLLATE NOCASE, id
                """;
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setString(2, status);
            statement.setString(3, type);
            statement.setString(4, type);
            try (ResultSet rows = statement.executeQuery()) {
                List<Equipment> result = new ArrayList<>();
                while (rows.next()) {
                    result.add(read(rows));
                }
                return result;
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Не удалось получить оборудование", error);
        }
    }

    public List<String> types() {
        String sql = "SELECT DISTINCT type FROM equipment ORDER BY type COLLATE NOCASE";
        try (Connection connection = database.open();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            List<String> result = new ArrayList<>();
            while (rows.next()) {
                result.add(rows.getString(1));
            }
            return result;
        } catch (SQLException error) {
            throw new IllegalStateException("Не удалось получить типы", error);
        }
    }

    public Optional<Equipment> findById(long id) {
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM equipment WHERE id = ?")) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(read(rows)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Не удалось получить оборудование", error);
        }
    }

    public Equipment create(EquipmentRequest request) {
        String sql = "INSERT INTO equipment (name, type, serial_number, status) VALUES (?, ?, ?, ?)";
        long id;
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setValues(statement, request);
            statement.executeUpdate();
            try (Statement lastId = connection.createStatement();
                 ResultSet rows = lastId.executeQuery("SELECT last_insert_rowid()")) {
                rows.next();
                id = rows.getLong(1);
            }
        } catch (SQLException error) {
            throw writeError(error);
        }
        return findById(id).orElseThrow();
    }

    public Equipment update(long id, String expectedStatus, EquipmentRequest request) {
        String sql = """
                UPDATE equipment SET name = ?, type = ?, serial_number = ?, status = ?
                WHERE id = ? AND status = ?
                """;
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            setValues(statement, request);
            statement.setLong(5, id);
            statement.setString(6, expectedStatus);
            if (statement.executeUpdate() == 0) {
                throw new ApiError.Problem(HttpStatus.CONFLICT, "Статус изменился. Обновите страницу");
            }
        } catch (SQLException error) {
            throw writeError(error);
        }
        return findById(id).orElseThrow();
    }

    public boolean delete(long id) {
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM equipment WHERE id = ? AND status != 'in_use'")) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException error) {
            throw new IllegalStateException("Не удалось удалить оборудование", error);
        }
    }

    private void setValues(PreparedStatement statement, EquipmentRequest request) throws SQLException {
        statement.setString(1, request.name().trim());
        statement.setString(2, request.type().trim());
        statement.setString(3, request.serialNumber().trim());
        statement.setString(4, request.status());
    }

    private Equipment read(ResultSet row) throws SQLException {
        return new Equipment(row.getLong("id"), row.getString("name"), row.getString("type"),
                row.getString("serial_number"), row.getString("status"));
    }

    private RuntimeException writeError(SQLException error) {
        if (error.getMessage() != null && error.getMessage().contains("equipment.serial_number")) {
            return new ApiError.Problem(HttpStatus.CONFLICT, "Серийный номер уже существует");
        }
        return new IllegalStateException("Не удалось сохранить оборудование", error);
    }
}
