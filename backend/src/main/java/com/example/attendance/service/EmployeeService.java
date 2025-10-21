package com.example.attendance.service;

import com.example.attendance.dto.AnnualLeaveSummary;
import com.example.attendance.dto.EmployeeRequest;
import com.example.attendance.dto.EmployeeResponse;
import com.example.attendance.entity.Employee;
import com.example.attendance.entity.LeaveType;
import com.example.attendance.repository.EmployeeRepository;
import com.example.attendance.repository.LeaveRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;

    public EmployeeService(EmployeeRepository employeeRepository, LeaveRequestRepository leaveRequestRepository) {
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
    }

    public EmployeeResponse create(EmployeeRequest request) {
        employeeRepository.findByEnglishNameIgnoreCase(request.getEnglishName())
                .ifPresent(e -> { throw new IllegalArgumentException("English name already exists"); });

        Employee employee = new Employee();
        employee.setChineseName(request.getChineseName());
        employee.setEnglishName(request.getEnglishName());
        employee.setHireDate(request.getHireDate());
        Employee saved = employeeRepository.save(employee);
        return toResponse(saved);
    }

    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        if (!employee.getEnglishName().equalsIgnoreCase(request.getEnglishName())) {
            employeeRepository.findByEnglishNameIgnoreCase(request.getEnglishName())
                    .ifPresent(e -> { throw new IllegalArgumentException("English name already exists"); });
        }
        employee.setChineseName(request.getChineseName());
        employee.setEnglishName(request.getEnglishName());
        employee.setHireDate(request.getHireDate());
        return toResponse(employeeRepository.save(employee));
    }

    public void delete(Long id) {
        employeeRepository.deleteById(id);
    }

    public EmployeeResponse get(Long id) {
        return employeeRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
    }

    public List<EmployeeResponse> list() {
        return employeeRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AnnualLeaveSummary getAnnualLeaveSummary(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        LocalDate today = LocalDate.now();
        LocalDate leaveYearStart = today.getMonthValue() >= 4
                ? LocalDate.of(today.getYear(), 4, 1)
                : LocalDate.of(today.getYear() - 1, 4, 1);
        LocalDate leaveYearEnd = leaveYearStart.plusYears(1);

        double monthlyAccrual = 1.0 / 12.0;
        double baseMonthlyAccrual = 10.0 / 12.0;
        double baseQuotaAccumulated = 0;
        double monthlyAccrualAccumulated = 0;

        YearMonth startMonth = YearMonth.from(leaveYearStart);
        YearMonth endMonth = YearMonth.from(leaveYearEnd.minusMonths(1));
        YearMonth current = startMonth;
        while (!current.isAfter(endMonth)) {
            LocalDate monthStartDate = current.atDay(1);
            if (!monthStartDate.isBefore(employee.getHireDate())) {
                long monthsSinceHire = monthsBetween(employee.getHireDate(), monthStartDate);
                if (monthsSinceHire >= 6 && baseQuotaAccumulated < 10.0) {
                    double add = Math.min(10.0 - baseQuotaAccumulated, baseMonthlyAccrual);
                    baseQuotaAccumulated += add;
                }
                monthlyAccrualAccumulated += monthlyAccrual;
            }
            current = current.plusMonths(1);
        }

        double totalQuota = baseQuotaAccumulated + monthlyAccrualAccumulated;
        LocalDateTime startDateTime = leaveYearStart.atStartOfDay();
        LocalDateTime endDateTime = leaveYearEnd.atStartOfDay();
        double usedHours = leaveRequestRepository.sumHoursByEmployeeAndTypeBetween(employeeId,
                LeaveType.ANNUAL.name(),
                startDateTime.toString(),
                endDateTime.toString());
        double usedDays = usedHours / 8.0;
        double remaining = Math.max(0, totalQuota - usedDays);
        return new AnnualLeaveSummary(roundToTwo(totalQuota), roundToTwo(usedDays), roundToTwo(remaining));
    }

    private long monthsBetween(LocalDate startInclusive, LocalDate endExclusive) {
        YearMonth start = YearMonth.from(startInclusive.withDayOfMonth(1));
        YearMonth end = YearMonth.from(endExclusive.withDayOfMonth(1));
        return (end.getYear() - start.getYear()) * 12L + (end.getMonthValue() - start.getMonthValue());
    }

    private double roundToTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(employee.getId(), employee.getChineseName(),
                employee.getEnglishName(), employee.getHireDate());
    }
}
