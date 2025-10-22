package com.example.attendance.dto;

import java.time.LocalDate;

public class EmployeeResponse {
    private Long id;
    private String chineseName;
    private String englishName;
    private LocalDate hireDate;

    public EmployeeResponse() {
    }

    public EmployeeResponse(Long id, String chineseName, String englishName, LocalDate hireDate) {
        this.id = id;
        this.chineseName = chineseName;
        this.englishName = englishName;
        this.hireDate = hireDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChineseName() {
        return chineseName;
    }

    public void setChineseName(String chineseName) {
        this.chineseName = chineseName;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }
}
