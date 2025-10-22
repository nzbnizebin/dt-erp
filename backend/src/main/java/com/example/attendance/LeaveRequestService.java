package com.example.attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class LeaveRequestService {
    private final Database database;
    private final EmployeeService employeeService;

    public LeaveRequestService(Database database, EmployeeService employeeService) {
        this.database = Objects.requireNonNull(database, "database");
        this.employeeService = Objects.requireNonNull(employeeService, "employeeService");
    }

    public LeaveRequest createLeaveRequest(Map<String, Object> payload) {
        String englishName = asString(payload.get("englishName"));
        String type = asString(payload.get("type"));
        LocalDateTime start = LocalDateTime.parse(asString(payload.get("startTime")));
        LocalDateTime end = LocalDateTime.parse(asString(payload.get("endTime")));
        double hours = asDouble(payload.get("hours"));

        if (hours < 1.0) {
            throw new IllegalArgumentException("Minimum leave duration is 1 hour");
        }
        if (!isWholeHour(hours)) {
            throw new IllegalArgumentException("Hours must be in whole hours");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("End time must not be before start time");
        }

        EmployeeService.Employee employee = employeeService.findByEnglishName(englishName)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        LeaveType leaveType = LeaveType.from(type);
        validateLeaveQuota(employee, leaveType, start, hours);

        String sql = String.format("INSERT INTO leave_request (employee_id, type, start_time, end_time, hours, created_at) " +
                        "VALUES (%d,'%s','%s','%s',%s,'%s');",
                employee.id(), leaveType.name(), start, end, hours, LocalDateTime.now());
        database.execute(sql);
        long id = Long.parseLong(database.query("SELECT last_insert_rowid() AS id;").get(0).get("id"));
        return new LeaveRequest(id, employee, leaveType, start, end, hours);
    }

    public PagedResult listLeaveRequests(Map<String, String> filters) {
        int page = parseInt(filters.getOrDefault("page", "0"), 0);
        int size = parseInt(filters.getOrDefault("size", "20"), 20);
        if (size <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        int offset = Math.max(page, 0) * size;

        StringBuilder where = new StringBuilder("WHERE 1=1");
        filters.computeIfPresent("englishName", (k, v) -> {
            where.append(" AND LOWER(e.english_name)='" + Database.escape(v.toLowerCase()) + "'");
            return v;
        });
        filters.computeIfPresent("type", (k, v) -> {
            where.append(" AND lr.type='" + Database.escape(v.toUpperCase()) + "'");
            return v;
        });
        filters.computeIfPresent("start", (k, v) -> {
            where.append(" AND lr.start_time >= '" + Database.escape(v) + "'");
            return v;
        });
        filters.computeIfPresent("end", (k, v) -> {
            where.append(" AND lr.end_time <= '" + Database.escape(v) + "'");
            return v;
        });

        String baseQuery = " FROM leave_request lr JOIN employee e ON lr.employee_id = e.id " + where;
        String dataSql = "SELECT lr.id, e.chinese_name, e.english_name, lr.type, lr.start_time, lr.end_time, lr.hours, lr.created_at"
                + baseQuery + " ORDER BY lr.start_time DESC LIMIT " + size + " OFFSET " + offset + ";";
        String countSql = "SELECT COUNT(*) AS total" + baseQuery + ";";

        List<Map<String, String>> rows = database.query(dataSql);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, String> row : rows) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", Long.parseLong(row.get("id")));
            item.put("chineseName", row.get("chinese_name"));
            item.put("englishName", row.get("english_name"));
            item.put("type", row.get("type"));
            item.put("startTime", row.get("start_time"));
            item.put("endTime", row.get("end_time"));
            item.put("hours", Double.parseDouble(row.get("hours")));
            item.put("createdAt", row.get("created_at"));
            items.add(item);
        }
        long total = Long.parseLong(database.query(countSql).get(0).getOrDefault("total", "0"));
        return new PagedResult(items, page, size, total);
    }

    private void validateLeaveQuota(EmployeeService.Employee employee, LeaveType type, LocalDateTime start, double hours) {
        switch (type) {
            case ANNUAL -> {
                EmployeeService.AnnualLeaveSummary summary = employeeService.calculateAnnualLeave(employee.id());
                double remainingHours = summary.remainingDays() * 8.0;
                if (hours > remainingHours + 1e-6) {
                    throw new IllegalArgumentException("Insufficient annual leave balance");
                }
            }
            case SICK -> {
                YearMonthRange month = YearMonthRange.of(start.toLocalDate());
                double used = employeeService.sumLeaveHours(employee.id(), "SICK",
                        month.start().atStartOfDay(), month.end().plusDays(1).atStartOfDay());
                double remaining = 8.0 - used;
                if (hours > remaining + 1e-6) {
                    throw new IllegalArgumentException("Monthly sick leave quota exceeded");
                }
            }
            default -> {
                // no-op for other leave types
            }
        }
    }

    private static boolean isWholeHour(double value) {
        return Math.abs(value - Math.rint(value)) < 1e-6;
    }

    private static String asString(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Missing required field");
        }
        return value.toString();
    }

    private static double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(asString(value));
    }

    private static int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public record LeaveRequest(long id, EmployeeService.Employee employee, LeaveType type,
                               LocalDateTime startTime, LocalDateTime endTime, double hours) {
    }

    public record PagedResult(List<Map<String, Object>> items, int page, int size, long total) {
    }

    public enum LeaveType {
        ANNUAL,
        SICK,
        PERSONAL,
        MARRIAGE,
        MATERNITY,
        OTHER;

        public static LeaveType from(String value) {
            if (value == null) {
                throw new IllegalArgumentException("Leave type is required");
            }
            String upper = value.trim().toUpperCase();
            return switch (upper) {
                case "ANNUAL" -> ANNUAL;
                case "SICK" -> SICK;
                case "PERSONAL" -> PERSONAL;
                case "MARRIAGE" -> MARRIAGE;
                case "MATERNITY" -> MATERNITY;
                default -> OTHER;
            };
        }
    }

    private record YearMonthRange(LocalDate start, LocalDate end) {
        static YearMonthRange of(LocalDate date) {
            LocalDate first = date.withDayOfMonth(1);
            LocalDate last = first.plusMonths(1).minusDays(1);
            return new YearMonthRange(first, last);
        }
    }
}
