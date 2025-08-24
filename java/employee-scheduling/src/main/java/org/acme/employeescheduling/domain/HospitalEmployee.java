package org.acme.employeescheduling.domain;

import java.util.Objects;
import java.util.Set;

import ai.timefold.solver.core.api.domain.lookup.PlanningId;

public class HospitalEmployee {
    @PlanningId
    private String memberId;
    
    // Group information
    private String groupName;
    private String workGroup;
    private boolean isSenior;
    
    // Availability constraints
    private Set<Integer> officialLeaveDays;
    private Set<Integer> excludedDays;
    private Set<Integer> selectedDays;
    
    // Special states
    private boolean lastDayShift; // Finished night shift on Aug 31st
    private boolean mesaiGrubuSenkronizasyonu; // Work group synchronization
    
    public HospitalEmployee() {
    }

    public HospitalEmployee(String memberId, String groupName, String workGroup, 
                          Set<Integer> officialLeaveDays, Set<Integer> excludedDays, 
                          Set<Integer> selectedDays, boolean lastDayShift) {
        this.memberId = memberId;
        this.groupName = groupName;
        this.workGroup = workGroup;
        this.officialLeaveDays = officialLeaveDays;
        this.excludedDays = excludedDays;
        this.selectedDays = selectedDays;
        this.lastDayShift = lastDayShift;
    }

    // Business logic methods
    public boolean isAvailableOnDay(int day) {
        return officialLeaveDays == null || !officialLeaveDays.contains(day);
    }
    
    public boolean shouldAvoidDay(int day) {
        return excludedDays != null && excludedDays.contains(day);
    }
    
    public boolean prefersDay(int day) {
        return selectedDays != null && selectedDays.contains(day);
    }
    
    public boolean canWorkAtWorkplace(String workplace) {
        // This will be determined by group permissions
        // For now, simple implementation
        return true;
    }

    // Getters and Setters
    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getWorkGroup() {
        return workGroup;
    }

    public void setWorkGroup(String workGroup) {
        this.workGroup = workGroup;
    }

    public boolean isSenior() {
        return isSenior;
    }

    public void setSenior(boolean senior) {
        isSenior = senior;
    }

    public Set<Integer> getOfficialLeaveDays() {
        return officialLeaveDays;
    }

    public void setOfficialLeaveDays(Set<Integer> officialLeaveDays) {
        this.officialLeaveDays = officialLeaveDays;
    }

    public Set<Integer> getExcludedDays() {
        return excludedDays;
    }

    public void setExcludedDays(Set<Integer> excludedDays) {
        this.excludedDays = excludedDays;
    }

    public Set<Integer> getSelectedDays() {
        return selectedDays;
    }

    public void setSelectedDays(Set<Integer> selectedDays) {
        this.selectedDays = selectedDays;
    }

    public boolean isLastDayShift() {
        return lastDayShift;
    }

    public void setLastDayShift(boolean lastDayShift) {
        this.lastDayShift = lastDayShift;
    }

    public boolean isMesaiGrubuSenkronizasyonu() {
        return mesaiGrubuSenkronizasyonu;
    }

    public void setMesaiGrubuSenkronizasyonu(boolean mesaiGrubuSenkronizasyonu) {
        this.mesaiGrubuSenkronizasyonu = mesaiGrubuSenkronizasyonu;
    }

    @Override
    public String toString() {
        return memberId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HospitalEmployee that)) return false;
        return Objects.equals(memberId, that.memberId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId);
    }
}
