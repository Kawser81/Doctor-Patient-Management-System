package com.example.doctor_patient_management_system.dto;

import java.time.LocalDate;
import java.util.List;

public class BookedSlotsDto {
    private LocalDate date;
    private List<Integer> slotIds;

    public BookedSlotsDto(LocalDate date, List<Integer> slotIds) {
        this.date = date;
        this.slotIds = slotIds;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public List<Integer> getSlotIds() {
        return slotIds;
    }

    public void setSlotIds(List<Integer> slotIds) {
        this.slotIds = slotIds;
    }
}

//mainly kun din koto gulo slot book hoise tar list