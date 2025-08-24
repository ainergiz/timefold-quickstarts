package org.acme.employeescheduling.domain;

import java.util.Objects;

/**
 * Configuration for a hospital department/workplace.
 * Defines staffing requirements for night and weekend shifts.
 */
public class WorkplaceConfig {
    private String name;
    // Staff needed for weekday NIGHT shifts
    private int weekdayNightStaffCount;
    // Staff needed for WEEKEND shifts
    private int weekendStaffCount;

    public WorkplaceConfig() {
    }

    public WorkplaceConfig(String name, int weekdayNightStaffCount, int weekendStaffCount) {
        this.name = name;
        this.weekdayNightStaffCount = weekdayNightStaffCount;
        this.weekendStaffCount = weekendStaffCount;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWeekdayNightStaffCount() {
        return weekdayNightStaffCount;
    }

    public void setWeekdayNightStaffCount(int weekdayNightStaffCount) {
        this.weekdayNightStaffCount = weekdayNightStaffCount;
    }

    public int getWeekendStaffCount() {
        return weekendStaffCount;
    }

    public void setWeekendStaffCount(int weekendStaffCount) {
        this.weekendStaffCount = weekendStaffCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WorkplaceConfig that = (WorkplaceConfig) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}