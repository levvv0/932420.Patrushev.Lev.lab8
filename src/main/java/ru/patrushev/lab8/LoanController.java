package ru.patrushev.lab8;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoanController {
    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    @GetMapping("/api/loans")
    public List<Loan> all(@RequestParam(defaultValue = "false") boolean active) {
        return service.findAll(active);
    }

    @PostMapping("/api/loans")
    public ResponseEntity<Loan> issue(@RequestBody LoanRequest request) {
        Loan loan = service.issue(request);
        return ResponseEntity.created(URI.create("/api/loans/" + loan.id())).body(loan);
    }

    @PutMapping("/api/loans/{id}/return")
    public Loan returnEquipment(@PathVariable long id) {
        return service.returnEquipment(id);
    }
}
