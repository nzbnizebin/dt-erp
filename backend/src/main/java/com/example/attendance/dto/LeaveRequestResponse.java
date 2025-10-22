package com.example.attendance.dto;

import java.time.LocalDateTime;

public class LeaveRequestResponse {
    private Long id;
    private Long employeeId;
    private String englishName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private double hours;
    private String type;

    public LeaveRequestResponse() {
    }

    public LeaveRequestResponse(Long id, Long employeeId, String englishName, LocalDateTime startTime,
                                LocalDateTime endTime, double hours, String type) {
        this.id = id;
        this.employeeId = employeeId;
        this.englishName = englishName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.hours = hours;
        this.type = type;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public double getHours() {
        return hours;
    }

    public void setHours(double hours) {
        this.hours = hours;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
