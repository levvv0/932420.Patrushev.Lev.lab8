package ru.patrushev.lab8;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EquipmentController {
    private final EquipmentService service;

    public EquipmentController(EquipmentService service) {
        this.service = service;
    }

    @GetMapping("/api/equipment")
    public List<Equipment> all(@RequestParam(required = false) String status,
                               @RequestParam(required = false) String type) {
        return service.findAll(status, type);
    }

    @GetMapping("/api/equipment/types")
    public List<String> types() {
        return service.types();
    }

    @GetMapping("/api/equipment/{id}")
    public Equipment one(@PathVariable long id) {
        return service.findById(id);
    }

    @PostMapping("/api/equipment")
    public ResponseEntity<Equipment> create(@RequestBody EquipmentRequest request) {
        Equipment equipment = service.create(request);
        return ResponseEntity.created(URI.create("/api/equipment/" + equipment.id())).body(equipment);
    }

    @PutMapping("/api/equipment/{id}")
    public Equipment update(@PathVariable long id, @RequestBody EquipmentRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/api/equipment/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
