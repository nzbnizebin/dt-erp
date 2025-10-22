package com.example.attendance.service;

import com.example.attendance.dto.LeaveRequestRequest;
import com.example.attendance.dto.LeaveRequestResponse;
import com.example.attendance.dto.PageResponse;
import com.example.attendance.entity.Employee;
import com.example.attendance.entity.LeaveRequest;
import com.example.attendance.entity.LeaveType;
import com.example.attendance.repository.EmployeeRepository;
import com.example.attendance.repository.LeaveRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Transactional
public class LeaveRequestService {
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;

    public LeaveRequestService(LeaveRequestRepository leaveRequestRepository,
                               EmployeeRepository employeeRepository,
                               EmployeeService employeeService) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.employeeService = employeeService;
    }

    public LeaveRequestResponse create(LeaveRequestRequest request) {
        if (request.getHours() < 1 || request.getHours() % 1 != 0) {
            throw new IllegalArgumentException("Hours must be a whole number and at least 1 hour");
        }
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        Employee employee = employeeRepository.findByEnglishNameIgnoreCase(request.getEnglishName())
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));

        LeaveType type = parseType(request.getType());

        if (type == LeaveType.SICK) {
            enforceSickLeaveQuota(employee, request);
        } else if (type == LeaveType.ANNUAL) {
            enforceAnnualLeaveQuota(employee, request);
        }

        LeaveRequest entity = new LeaveRequest();
        entity.setEmployee(employee);
        entity.setStartTime(request.getStartTime());
        entity.setEndTime(request.getEndTime());
        entity.setHours(request.getHours());
        entity.setType(type);
        LeaveRequest saved = leaveRequestRepository.save(entity);
        return toResponse(saved);
    }

    public PageResponse<LeaveRequestResponse> search(String englishName, String type, LocalDateTime start,
                                                     LocalDateTime end, int page, int size) {
        LeaveType leaveType = type != null ? parseType(type) : null;
        Page<LeaveRequest> result = leaveRequestRepository.search(
                englishName,
                leaveType,
                start,
                end,
                PageRequest.of(page, size)
        );
        return new PageResponse<>(result.map(this::toResponse).getContent(), result.getNumber(),
                result.getSize(), result.getTotalElements());
    }

    public void delete(Long id) {
        leaveRequestRepository.deleteById(id);
    }

    private void enforceSickLeaveQuota(Employee employee, LeaveRequestRequest request) {
        LocalDate month = request.getStartTime().toLocalDate().withDayOfMonth(1);
        String key = month.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        double used = leaveRequestRepository.sumHoursByEmployeeAndTypeAndMonth(employee.getId(), LeaveType.SICK.name(), key);
        if (used + request.getHours() > 8.0) {
            throw new IllegalArgumentException("Exceeds monthly sick leave quota (1 day)");
        }
    }

    private void enforceAnnualLeaveQuota(Employee employee, LeaveRequestRequest request) {
        double remainingDays = employeeService.getAnnualLeaveSummary(employee.getId()).getRemainingDays();
        double requestDays = request.getHours() / 8.0;
        if (requestDays > remainingDays + 1e-6) {
            throw new IllegalArgumentException("Insufficient annual leave quota");
        }
    }

    private LeaveType parseType(String raw) {
        try {
            return LeaveType.valueOf(raw.trim().toUpperCase());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unsupported leave type: " + raw);
        }
    }

    private LeaveRequestResponse toResponse(LeaveRequest entity) {
        return new LeaveRequestResponse(entity.getId(), entity.getEmployee().getId(),
                entity.getEmployee().getEnglishName(), entity.getStartTime(), entity.getEndTime(),
                entity.getHours(), entity.getType().name());
    }
}
