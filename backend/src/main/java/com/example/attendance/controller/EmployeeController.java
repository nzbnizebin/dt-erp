package com.example.attendance.controller;

import com.example.attendance.dto.AnnualLeaveSummary;
import com.example.attendance.dto.EmployeeRequest;
import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> list() {
        return ResponseEntity.ok(employeeService.list());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.get(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<EmployeeResponse> update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/annual-leave")
    public ResponseEntity<AnnualLeaveSummary> annualLeave(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getAnnualLeaveSummary(id));
    }
}
