package org.acme.employeescheduling.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.lookup.PlanningId;
import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class HospitalShift {
    @PlanningId
    private String id;
    
    private int day; // Day of month (1-30 for September)
    private ShiftType shiftType;
    private String workplace; // Where the shift takes place
    private String requiredWorkGroup; // Required specialty/work group
    
    @PlanningVariable
    private HospitalEmployee employee;

    public HospitalShift() {
    }

    public HospitalShift(String id, int day, ShiftType shiftType, String workplace, String requiredWorkGroup) {
        this.id = id;
        this.day = day;
        this.shiftType = shiftType;
        this.workplace = workplace;
        this.requiredWorkGroup = requiredWorkGroup;
    }

    // Business logic methods
    public boolean isDayShift() {
        return shiftType == ShiftType.DAY_SHIFT;
    }
    
    public boolean isNightShift() {
        return shiftType == ShiftType.NIGHT_SHIFT;
    }
    
    public boolean isWeekendShift() {
        return shiftType == ShiftType.WEEKEND_SHIFT;
    }
    
    public boolean isWeekend() {
        // September 2025: Saturdays (7,14,21,28) and Sundays (1,8,15,22,29)
        return day == 1 || day == 7 || day == 8 || day == 14 || day == 15 || 
               day == 21 || day == 22 || day == 28 || day == 29;
    }
    
    public LocalDateTime getStartTime() {
        LocalDate date = LocalDate.of(2025, 9, day);
        return switch (shiftType) {
            case DAY_SHIFT -> date.atTime(9, 0);
            case NIGHT_SHIFT -> date.atTime(17, 0); // 5 PM
            case WEEKEND_SHIFT -> date.atTime(9, 0);
        };
    }
    
    public LocalDateTime getEndTime() {
        LocalDate date = LocalDate.of(2025, 9, day);
        return switch (shiftType) {
            case DAY_SHIFT -> date.atTime(17, 0); // 5 PM same day
            case NIGHT_SHIFT -> date.plusDays(1).atTime(9, 0); // 9 AM next day
            case WEEKEND_SHIFT -> date.plusDays(1).atTime(9, 0); // 9 AM next day
        };
    }
    
    public int getNextDay() {
        return switch (shiftType) {
            case DAY_SHIFT -> day; // Same day
            case NIGHT_SHIFT, WEEKEND_SHIFT -> day + 1; // Next day
        };
    }
    
    public int getShiftDurationHours() {
        return switch (shiftType) {
            case DAY_SHIFT -> 8; // 9am-5pm
            case NIGHT_SHIFT -> 16; // 5pm-9am next day
            case WEEKEND_SHIFT -> 24; // 9am-9am next day
        };
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public ShiftType getShiftType() {
        return shiftType;
    }

    public void setShiftType(ShiftType shiftType) {
        this.shiftType = shiftType;
    }

    public String getWorkplace() {
        return workplace;
    }

    public void setWorkplace(String workplace) {
        this.workplace = workplace;
    }

    public String getRequiredWorkGroup() {
        return requiredWorkGroup;
    }

    public void setRequiredWorkGroup(String requiredWorkGroup) {
        this.requiredWorkGroup = requiredWorkGroup;
    }

    public HospitalEmployee getEmployee() {
        return employee;
    }

    public void setEmployee(HospitalEmployee employee) {
        this.employee = employee;
    }

    @Override
    public String toString() {
        return String.format("%s Day %d %s at %s", shiftType, day, 
                           requiredWorkGroup != null ? requiredWorkGroup : "General", 
                           workplace != null ? workplace : "Hospital");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HospitalShift that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
