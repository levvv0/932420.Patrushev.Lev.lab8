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
public class LoanRepository {
    private final Database database;

    public LoanRepository(Database database) {
        this.database = database;
    }

    public List<Loan> findAll(boolean activeOnly) {
        String sql = """
                SELECT l.*, e.name AS equipment_name FROM loans l
                JOIN equipment e ON e.id = l.equipment_id
                """ + (activeOnly ? "WHERE l.return_date IS NULL " : "") +
                "ORDER BY l.issue_date DESC, l.id DESC";
        try (Connection connection = database.open();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            List<Loan> result = new ArrayList<>();
            while (rows.next()) {
                result.add(read(rows));
            }
            return result;
        } catch (SQLException error) {
            throw new IllegalStateException("Не удалось получить выдачи", error);
        }
    }

    public Optional<Loan> findById(long id) {
        String sql = """
                SELECT l.*, e.name AS equipment_name FROM loans l
                JOIN equipment e ON e.id = l.equipment_id WHERE l.id = ?
                """;
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? Optional.of(read(rows)) : Optional.empty();
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Не удалось получить выдачу", error);
        }
    }

    public Loan issue(LoanRequest request) {
        long id;
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE equipment SET status = 'in_use' WHERE id = ? AND status = 'available'")) {
                    update.setLong(1, request.equipmentId());
                    if (update.executeUpdate() == 0) {
                        throw unavailable(connection, request.equipmentId());
                    }
                }
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO loans (equipment_id, borrower) VALUES (?, ?)")) {
                    insert.setLong(1, request.equipmentId());
                    insert.setString(2, request.borrower().trim());
                    insert.executeUpdate();
                }
                try (Statement lastId = connection.createStatement();
                     ResultSet rows = lastId.executeQuery("SELECT last_insert_rowid()")) {
                    rows.next();
                    id = rows.getLong(1);
                }
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Не удалось оформить выдачу", error);
        }
        return findById(id).orElseThrow();
    }

    public Loan returnEquipment(long id) {
        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                long equipmentId;
                try (PreparedStatement select = connection.prepareStatement(
                        "SELECT equipment_id, return_date FROM loans WHERE id = ?")) {
                    select.setLong(1, id);
                    try (ResultSet row = select.executeQuery()) {
                        if (!row.next()) {
                            throw new ApiError.Problem(HttpStatus.NOT_FOUND, "Выдача не найдена");
                        }
                        if (row.getString("return_date") != null) {
                            throw new ApiError.Problem(HttpStatus.CONFLICT, "Оборудование уже возвращено");
                        }
                        equipmentId = row.getLong("equipment_id");
                    }
                }
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE loans SET return_date = strftime('%Y-%m-%dT%H:%M:%fZ', 'now')
                        WHERE id = ? AND return_date IS NULL
                        """)) {
                    update.setLong(1, id);
                    if (update.executeUpdate() == 0) {
                        throw new ApiError.Problem(HttpStatus.CONFLICT, "Оборудование уже возвращено");
                    }
                }
                try (PreparedStatement update = connection.prepareStatement(
                        "UPDATE equipment SET status = 'available' WHERE id = ? AND status = 'in_use'")) {
                    update.setLong(1, equipmentId);
                    if (update.executeUpdate() == 0) {
                        throw new ApiError.Problem(HttpStatus.CONFLICT, "Статус оборудования изменился");
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            }
        } catch (SQLException error) {
            throw new IllegalStateException("Не удалось оформить возврат", error);
        }
        return findById(id).orElseThrow();
    }

    private ApiError.Problem unavailable(Connection connection, long equipmentId) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement("SELECT id FROM equipment WHERE id = ?")) {
            select.setLong(1, equipmentId);
            try (ResultSet row = select.executeQuery()) {
                return row.next()
                        ? new ApiError.Problem(HttpStatus.CONFLICT, "Оборудование уже выдано или на обслуживании")
                        : new ApiError.Problem(HttpStatus.NOT_FOUND, "Оборудование не найдено");
            }
        }
    }

    private Loan read(ResultSet row) throws SQLException {
        return new Loan(row.getLong("id"), row.getLong("equipment_id"), row.getString("borrower"),
                row.getString("issue_date"), row.getString("return_date"), row.getString("equipment_name"));
    }
}
