package org.acme.employeescheduling.domain;

import java.util.List;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.domain.solution.PlanningEntityCollectionProperty;
import ai.timefold.solver.core.api.domain.solution.PlanningScore;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolverStatus;

@PlanningSolution
public class HospitalSchedule {
    
    private int month; // 9 for September
    private int year;  // 2025
    
    @ProblemFactCollectionProperty
    @ValueRangeProvider
    private List<HospitalEmployee> employees;
    
    @PlanningEntityCollectionProperty
    private List<HospitalShift> shifts;
    
    // Configuration data
    @ProblemFactCollectionProperty
    private List<GroupConfig> groups;
    
    @ProblemFactCollectionProperty 
    private List<WorkplaceConfig> workplaces;
    
    @ProblemFactCollectionProperty
    private List<WorkGroupConfig> workGroups;
    
    @PlanningScore
    private HardSoftScore score;
    
    private ConstraintWeightOverrides<HardSoftScore> constraintWeightOverrides;
    
    private SolverStatus solverStatus;

    public HospitalSchedule() {
    }

    public HospitalSchedule(int month, int year, List<HospitalEmployee> employees, List<HospitalShift> shifts) {
        this.month = month;
        this.year = year;
        this.employees = employees;
        this.shifts = shifts;
    }

    // Business methods
    public boolean isWeekend(int day) {
        // September 2025: Saturdays (7,14,21,28) and Sundays (1,8,15,22,29)
        return day == 1 || day == 7 || day == 8 || day == 14 || day == 15 || 
               day == 21 || day == 22 || day == 28 || day == 29;
    }
    
    public int getEmployeeCount() {
        return employees != null ? employees.size() : 0;
    }
    
    public int getShiftCount() {
        return shifts != null ? shifts.size() : 0;
    }
    
    public int getUnassignedShiftCount() {
        if (shifts == null) return 0;
        return (int) shifts.stream()
                .filter(shift -> shift.getEmployee() == null)
                .count();
    }

    // Getters and Setters
    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public List<HospitalEmployee> getEmployees() {
        return employees;
    }

    public void setEmployees(List<HospitalEmployee> employees) {
        this.employees = employees;
    }

    public List<HospitalShift> getShifts() {
        return shifts;
    }

    public void setShifts(List<HospitalShift> shifts) {
        this.shifts = shifts;
    }

    public List<GroupConfig> getGroups() {
        return groups;
    }

    public void setGroups(List<GroupConfig> groups) {
        this.groups = groups;
    }

    public List<WorkplaceConfig> getWorkplaces() {
        return workplaces;
    }

    public void setWorkplaces(List<WorkplaceConfig> workplaces) {
        this.workplaces = workplaces;
    }

    public List<WorkGroupConfig> getWorkGroups() {
        return workGroups;
    }

    public void setWorkGroups(List<WorkGroupConfig> workGroups) {
        this.workGroups = workGroups;
    }

    public HardSoftScore getScore() {
        return score;
    }

    public void setScore(HardSoftScore score) {
        this.score = score;
    }

    public ConstraintWeightOverrides<HardSoftScore> getConstraintWeightOverrides() {
        return constraintWeightOverrides;
    }

    public void setConstraintWeightOverrides(ConstraintWeightOverrides<HardSoftScore> constraintWeightOverrides) {
        this.constraintWeightOverrides = constraintWeightOverrides;
    }

    public SolverStatus getSolverStatus() {
        return solverStatus;
    }

    public void setSolverStatus(SolverStatus solverStatus) {
        this.solverStatus = solverStatus;
    }

    @Override
    public String toString() {
        return String.format("HospitalSchedule[%d/%d, %d employees, %d shifts, score: %s]",
                           month, year, getEmployeeCount(), getShiftCount(), 
                           score != null ? score : "?");
    }
}
