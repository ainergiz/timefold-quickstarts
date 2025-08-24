package org.acme.employeescheduling.domain;

import java.util.Objects;

/**
 * Configuration for work groups (medical specialties).
 * 
 * Defines the number of staff needed from each work group for weekday day shifts.
 * Used in Rule 7 to limit work group assignments per day.
 * 
 * Dependencies: None - standalone configuration class
 * Key Features:
 * - Work group name (medical specialty)
 * - Number of staff needed for weekday day shifts (9-5)
 * - Supports work group limit constraints
 */
public class WorkGroupConfig {
    private String workGroup; // e.g., "Gastroenteroloji", "Kardiyoloji"
    private int neededCount; // Number of staff needed for weekday day shifts
    
    public WorkGroupConfig() {
    }
    
    public WorkGroupConfig(String workGroup, int neededCount) {
        this.workGroup = workGroup;
        this.neededCount = neededCount;
    }
    
    // Getters and Setters
    public String getWorkGroup() {
        return workGroup;
    }
    
    public void setWorkGroup(String workGroup) {
        this.workGroup = workGroup;
    }
    
    public int getNeededCount() {
        return neededCount;
    }
    
    public void setNeededCount(int neededCount) {
        this.neededCount = neededCount;
    }
    
    /**
     * Calculate maximum number of people from this work group 
     * that can be assigned to night shifts on any given day.
     * 
     * Rule 7 logic: maxAssignable = totalInGroup - neededCount
     */
    public int getMaxAssignablePerDay(int totalInGroup) {
        return Math.max(0, totalInGroup - neededCount);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorkGroupConfig that)) return false;
        return Objects.equals(workGroup, that.workGroup);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(workGroup);
    }
    
    @Override
    public String toString() {
        return String.format("%s(needed:%d)", workGroup, neededCount);
    }
}
