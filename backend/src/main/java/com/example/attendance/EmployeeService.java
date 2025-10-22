package com.example.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class EmployeeService {
    private final Database database;

    public EmployeeService(Database database) {
        this.database = Objects.requireNonNull(database, "database");
    }

    public Employee createEmployee(String chineseName, String englishName, LocalDate hireDate) {
        requireNonBlank(chineseName, "Chinese name is required");
        requireNonBlank(englishName, "English name is required");
        Objects.requireNonNull(hireDate, "hireDate");
        List<Map<String, String>> existing = database.query("SELECT id FROM employee WHERE LOWER(english_name)='" +
                Database.escape(englishName.toLowerCase()) + "';");
        if (!existing.isEmpty()) {
            throw new IllegalArgumentException("English name already exists");
        }
        String sql = String.format("INSERT INTO employee (chinese_name, english_name, hire_date) " +
                        "VALUES ('%s','%s','%s');",
                Database.escape(chineseName), Database.escape(englishName), hireDate);
        database.execute(sql);
        long id = Long.parseLong(database.query("SELECT last_insert_rowid() AS id;").get(0).get("id"));
        return new Employee(id, chineseName, englishName, hireDate);
    }

    public List<Employee> listEmployees() {
        List<Map<String, String>> rows = database.query(
                "SELECT id, chinese_name, english_name, hire_date FROM employee ORDER BY id;");
        List<Employee> employees = new ArrayList<>();
        for (Map<String, String> row : rows) {
            employees.add(mapEmployee(row));
        }
        return employees;
    }

    public Employee updateEmployee(long id, String chineseName, String englishName, LocalDate hireDate) {
        Employee existing = getEmployee(id);
        requireNonBlank(chineseName, "Chinese name is required");
        requireNonBlank(englishName, "English name is required");
        Objects.requireNonNull(hireDate, "hireDate");
        List<Map<String, String>> conflicts = database.query(
                "SELECT id FROM employee WHERE LOWER(english_name)='" +
                        Database.escape(englishName.toLowerCase()) + "' AND id<>" + id + ";");
        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("English name already exists");
        }
        String sql = String.format(
                "UPDATE employee SET chinese_name='%s', english_name='%s', hire_date='%s' WHERE id=%d;",
                Database.escape(chineseName), Database.escape(englishName), hireDate, id);
        database.execute(sql);
        return new Employee(existing.id(), chineseName, englishName, hireDate);
    }

    public void deleteEmployee(long id) {
        getEmployee(id);
        database.execute("DELETE FROM employee WHERE id=" + id + ";");
    }

    public Employee getEmployee(long id) {
        List<Map<String, String>> rows = database.query(
                "SELECT id, chinese_name, english_name, hire_date FROM employee WHERE id=" + id + ";");
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Employee not found");
        }
        return mapEmployee(rows.get(0));
    }

    public Optional<Employee> findByEnglishName(String englishName) {
        List<Map<String, String>> rows = database.query("SELECT id, chinese_name, english_name, hire_date FROM employee " +
                "WHERE LOWER(english_name)='" + Database.escape(englishName.toLowerCase()) + "';");
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapEmployee(rows.get(0)));
    }

    public AnnualLeaveSummary calculateAnnualLeave(long employeeId) {
        Employee employee = getEmployee(employeeId);
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
            if (!monthStartDate.isBefore(employee.hireDate())) {
                long monthsSinceHire = monthsBetween(employee.hireDate(), monthStartDate);
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
        double usedHours = sumLeaveHours(employeeId, "ANNUAL", startDateTime, endDateTime);
        double usedDays = usedHours / 8.0;
        double remaining = Math.max(0, totalQuota - usedDays);
        return new AnnualLeaveSummary(round(totalQuota), round(usedDays), round(remaining));
    }

    double sumLeaveHours(long employeeId, String type, LocalDateTime start, LocalDateTime end) {
        String sql = String.format("SELECT IFNULL(SUM(hours),0) AS total FROM leave_request WHERE employee_id=%d " +
                        "AND type='%s' AND start_time >= '%s' AND start_time < '%s';",
                employeeId, Database.escape(type), start, end);
        List<Map<String, String>> rows = database.query(sql);
        if (rows.isEmpty()) {
            return 0;
        }
        return Double.parseDouble(rows.get(0).getOrDefault("total", "0"));
    }

    private long monthsBetween(LocalDate startInclusive, LocalDate monthStart) {
        YearMonth start = YearMonth.from(startInclusive.withDayOfMonth(1));
        YearMonth end = YearMonth.from(monthStart.withDayOfMonth(1));
        return (end.getYear() - start.getYear()) * 12L + (end.getMonthValue() - start.getMonthValue());
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private static void requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private Employee mapEmployee(Map<String, String> row) {
        return new Employee(Long.parseLong(row.get("id")), row.get("chinese_name"),
                row.get("english_name"), LocalDate.parse(row.get("hire_date")));
    }

    public record Employee(long id, String chineseName, String englishName, LocalDate hireDate) {
    }

    public record AnnualLeaveSummary(double totalQuota, double usedDays, double remainingDays) {
    }
}
