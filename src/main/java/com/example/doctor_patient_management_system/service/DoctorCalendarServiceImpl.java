package com.example.doctor_patient_management_system.service;

import com.example.doctor_patient_management_system.dto.BookedSlotsDto;
import com.example.doctor_patient_management_system.dto.DayStatus;
import com.example.doctor_patient_management_system.model.Appointment;
import com.example.doctor_patient_management_system.model.enumeration.AppointmentStatus;
import com.example.doctor_patient_management_system.repository.AppointmentRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class DoctorCalendarServiceImpl implements  DoctorCalendarService {

    private final DoctorAvailabilityServiceImpl doctorAvailabilityService;
    private final AppointmentRepository appointmentRepository;

    public DoctorCalendarServiceImpl(DoctorAvailabilityServiceImpl doctorAvailabilityService, AppointmentRepository appointmentRepository) {
        this.doctorAvailabilityService = doctorAvailabilityService;
        this.appointmentRepository = appointmentRepository;
    }

    @Override
    public List<DayStatus> getCalendarData(Long doctorId, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();
        LocalDate today = LocalDate.now();

        List<BookedSlotsDto> bookedSlotsList = getBookedSlotsMap(doctorId, startDate, endDate);

        //[
        //    BookedSlotsDto { date: 2026-01-05, slotIds: [5, 8, 12] },
        //    BookedSlotsDto { date: 2026-01-10, slotIds: [3, 7] },
        //    BookedSlotsDto { date: 2026-01-20, slotIds: [15] }
        //]

        List<LocalDate> allDates = getAllDatesInMonth(year, month);

//        [
//            2026-01-01,  // Wednesday
//            2026-01-02,  // Thursday
//            026-01-03,  // Friday (OFF DAY)
//            2026-01-04,  // Saturday (OFF DAY)
//            2026-01-05,  // Sunday
//            ...
//        ]

        return buildDayStatuses(allDates, bookedSlotsList, today, doctorId);
    }

    private List<BookedSlotsDto> getBookedSlotsMap(Long doctorId, LocalDate startDate, LocalDate endDate) {
        List<Appointment> appointments = appointmentRepository
                .findByDoctorIdAndAppointmentDateBetween(doctorId, startDate, endDate);

        return appointments.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFIRMED)
                .collect(Collectors.groupingBy(
                        Appointment::getAppointmentDate,
                        Collectors.mapping(Appointment::getSlotId, Collectors.toList())
                ))
                .entrySet().stream()
                .map(entry -> new BookedSlotsDto(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

    }

    private List<LocalDate> getAllDatesInMonth(int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        int daysInMonth = YearMonth.of(year, month).lengthOfMonth();
        return IntStream.rangeClosed(1, daysInMonth)
                .mapToObj(day -> LocalDate.of(year, month, day))
                .collect(Collectors.toList());
    }

    private List<DayStatus> buildDayStatuses(List<LocalDate> allDates,
                                             List<BookedSlotsDto> bookedSlotsList,
                                             LocalDate today,
                                             Long doctorId) {
        List<DayStatus> days = new ArrayList<>();
        int totalSlots = doctorAvailabilityService.totalSlotsPerDay(doctorId);

        for (LocalDate date : allDates) {
            boolean isPast = date.isBefore(today);
            boolean isOff = doctorAvailabilityService.isOffDay(date, doctorId);

            // Find booked slots for this date from the DTO list
            int bookedCount = bookedSlotsList.stream()
                    .filter(dto -> dto.getDate().equals(date))
                    .findFirst()
                    .map(dto -> dto.getSlotIds().size())
                    .orElse(0);

            int available = totalSlots - bookedCount;
            String status = isPast ? "past" : isOff ? "off" : bookedCount == 0 ? "available" : bookedCount < totalSlots ? "partial" : "full";
            boolean isTodayFlag = date.isEqual(today);
            String dayName = date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            days.add(new DayStatus(date, status, bookedCount, available, isTodayFlag, dayName));
        }

//        DayStatus {
//            date: 2026-01-01,
//            status: "past",
//            bookedCount: 0,
//            availableCount: 24,
//            isToday: false,
//            dayName: "Wednesday"
//        }


        return days;

        //Date: 2026-01-12, isPast: false (today is not before today), isOff: true (Sunday), bookedCount: 5, available: 10 - 5 = 5
        //status: "off" (because isOff = true), isToday: true, dayName: "Sunday"
    }
}
