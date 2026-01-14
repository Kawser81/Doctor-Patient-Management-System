package com.example.doctor_patient_management_system.dto;

public class TimeSlot {
    private int id;
    private String slotName;
    private String startTime;
    private String endTime;
    private String session;

    public TimeSlot(int id, String slotName, String startTime, String endTime, String session) {
        this.id = id;
        this.slotName = slotName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.session = session;
    }

    public int getId() {
        return id;
    }

    public String getSlotName() {
        return slotName;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getSession() {
        return session;
    }
}