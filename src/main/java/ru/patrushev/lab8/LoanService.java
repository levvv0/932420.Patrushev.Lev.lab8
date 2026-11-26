package ru.patrushev.lab8;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LoanService {
    private final LoanRepository repository;

    public LoanService(LoanRepository repository) {
        this.repository = repository;
    }

    public List<Loan> findAll(boolean activeOnly) {
        return repository.findAll(activeOnly);
    }

    public Loan issue(LoanRequest request) {
        if (request == null || request.equipmentId() == null || request.equipmentId() <= 0 ||
                request.borrower() == null || request.borrower().isBlank()) {
            throw new ApiError.Problem(HttpStatus.BAD_REQUEST, "Укажите оборудование и получателя");
        }
        return repository.issue(request);
    }

    public Loan returnEquipment(long id) {
        return repository.returnEquipment(id);
    }
}
