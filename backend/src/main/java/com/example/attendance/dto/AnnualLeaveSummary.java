package com.example.attendance.dto;

public class AnnualLeaveSummary {
    private double totalQuotaDays;
    private double usedDays;
    private double remainingDays;

    public AnnualLeaveSummary() {
    }

    public AnnualLeaveSummary(double totalQuotaDays, double usedDays, double remainingDays) {
        this.totalQuotaDays = totalQuotaDays;
        this.usedDays = usedDays;
        this.remainingDays = remainingDays;
    }

    public double getTotalQuotaDays() {
        return totalQuotaDays;
    }

    public void setTotalQuotaDays(double totalQuotaDays) {
        this.totalQuotaDays = totalQuotaDays;
    }

    public double getUsedDays() {
        return usedDays;
    }

    public void setUsedDays(double usedDays) {
        this.usedDays = usedDays;
    }

    public double getRemainingDays() {
        return remainingDays;
    }

    public void setRemainingDays(double remainingDays) {
        this.remainingDays = remainingDays;
    }
}
