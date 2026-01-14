package com.example.doctor_patient_management_system.dto;

import java.time.LocalDate;

public class DayStatus {
    private LocalDate date;
    private String status;
    private int bookedCount;
    private int availableCount;
    private boolean isToday;
    private String dayName;

    public DayStatus(LocalDate date, String status, int bookedCount, int availableCount, boolean isToday, String dayName) {
        this.date = date;
        this.status = status;
        this.bookedCount = bookedCount;
        this.availableCount = availableCount;
        this.isToday = isToday;
        this.dayName = dayName;
    }

    public LocalDate getDate() { return date; }
    public String getStatus() { return status; }
    public int getBookedCount() { return bookedCount; }
    public int getAvailableCount() { return availableCount; }
    public boolean isToday() { return isToday; }
    public String getDayName() { return dayName; }
}