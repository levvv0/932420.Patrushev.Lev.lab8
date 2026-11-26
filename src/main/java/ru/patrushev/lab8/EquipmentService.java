package ru.patrushev.lab8;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EquipmentService {
    private final EquipmentRepository repository;

    public EquipmentService(EquipmentRepository repository) {
        this.repository = repository;
    }

    public List<Equipment> findAll(String status, String type) {
        if (status != null && !status.isBlank() && !validStatus(status)) {
            throw new ApiError.Problem(HttpStatus.BAD_REQUEST, "Неизвестный статус");
        }
        return repository.findAll(status == null || status.isBlank() ? null : status,
                type == null || type.isBlank() ? null : type);
    }

    public List<String> types() {
        return repository.types();
    }

    public Equipment findById(long id) {
        return repository.findById(id).orElseThrow(() ->
                new ApiError.Problem(HttpStatus.NOT_FOUND, "Оборудование не найдено"));
    }

    public Equipment create(EquipmentRequest request) {
        validate(request);
        if ("in_use".equals(request.status())) {
            throw new ApiError.Problem(HttpStatus.BAD_REQUEST, "Выдайте оборудование через форму выдачи");
        }
        return repository.create(request);
    }

    public Equipment update(long id, EquipmentRequest request) {
        validate(request);
        Equipment previous = findById(id);
        if ("in_use".equals(previous.status()) != "in_use".equals(request.status())) {
            throw new ApiError.Problem(HttpStatus.CONFLICT, "Статус выдачи меняется только при выдаче или возврате");
        }
        return repository.update(id, previous.status(), request);
    }

    public void delete(long id) {
        Equipment current = findById(id);
        if ("in_use".equals(current.status()) || !repository.delete(id)) {
            throw new ApiError.Problem(HttpStatus.CONFLICT, "Сначала верните выданное оборудование");
        }
    }

    private boolean validStatus(String status) {
        return "available".equals(status) || "in_use".equals(status) || "maintenance".equals(status);
    }

    private void validate(EquipmentRequest request) {
        if (request == null || request.name() == null || request.name().isBlank() ||
                request.type() == null || request.type().isBlank() ||
                request.serialNumber() == null || request.serialNumber().isBlank() ||
                !validStatus(request.status())) {
            throw new ApiError.Problem(HttpStatus.BAD_REQUEST, "Заполните название, тип, серийный номер и статус");
        }
    }
}
