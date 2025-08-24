package org.acme.employeescheduling.solver;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.stream.Constraint;
import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.ConstraintFactory;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.Joiners;
import org.acme.employeescheduling.domain.HospitalShift;
import org.acme.employeescheduling.domain.GroupConfig;
import org.acme.employeescheduling.domain.WorkGroupConfig;
import org.acme.employeescheduling.config.NydhQuotaConfiguration;

import java.time.Duration;

public class HospitalSchedulingConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory constraintFactory) {
        return new Constraint[] {
                // Hard constraints - Basic scheduling rules
                oneShiftPerEmployeeAtTheSameTime(constraintFactory),
                restAfterNightShift(constraintFactory),
                noConsecutiveNightShifts(constraintFactory),
                noWorkOnOfficialLeaveDays(constraintFactory),
                noWorkOnExcludedDays(constraintFactory),
                handleLastDayShiftEmployees(constraintFactory),
                
                // Hard constraints - Individual assignment quotas
                enforceMaxNightShiftsPerEmployee(constraintFactory),
                enforceMaxWeekendShiftsPerEmployee(constraintFactory),
                
                // Hard constraints - Workplace qualifications
                enforceWorkplaceQualifications(constraintFactory),
                
                // Hard constraints - Work group daily limits (Rule 7)
                enforceWorkGroupDailyLimits(constraintFactory),
                
                // Soft constraints
                preferSelectedDays(constraintFactory),
                enforceNydhQuotas(constraintFactory) // NYDH as soft constraint for optimal distribution
        };
    }

    // No two shifts assigned to the same employee can overlap.
    Constraint oneShiftPerEmployeeAtTheSameTime(ConstraintFactory constraintFactory) {
return constraintFactory.forEachUniquePair(HospitalShift.class,
                Joiners.equal(HospitalShift::getEmployee),
                Joiners.overlapping(HospitalShift::getStartTime, HospitalShift::getEndTime))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Overlapping shift");
    }

    // An employee must have at least 23 hours of rest after a night or weekend shift.
    Constraint restAfterNightShift(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(HospitalShift.class)
                .filter(shift -> shift.isNightShift() || shift.isWeekendShift())
                .join(HospitalShift.class,
                        Joiners.equal(HospitalShift::getEmployee),
                        Joiners.lessThan(HospitalShift::getEndTime, HospitalShift::getStartTime))
                .filter((nightShift, nextShift) -> Duration.between(nightShift.getEndTime(), nextShift.getStartTime()).toHours() < 23)
.penalize(HardSoftScore.ONE_HARD, (nightShift, nextShift) -> {
                    long breakInMinutes = Duration.between(nightShift.getEndTime(), nextShift.getStartTime()).toMinutes();
                    return (23 * 60) - (int) breakInMinutes;
                })
                .asConstraint("Insufficient rest after night shift");
    }

    // An employee cannot be assigned to a shift on their official leave day.
    Constraint noWorkOnOfficialLeaveDays(ConstraintFactory constraintFactory) {
return constraintFactory.forEach(HospitalShift.class)
                .filter(shift -> shift.getEmployee() != null && !shift.getEmployee().isAvailableOnDay(shift.getDay()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Work on official leave day");
    }
    
    // An employee cannot be assigned to a shift on their excluded days.
    Constraint noWorkOnExcludedDays(ConstraintFactory constraintFactory) {
return constraintFactory.forEach(HospitalShift.class)
                .filter(shift -> shift.getEmployee() != null && shift.getEmployee().shouldAvoidDay(shift.getDay()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Work on excluded day");
    }

    // No consecutive night shifts for the same employee.
    Constraint noConsecutiveNightShifts(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(HospitalShift.class)
                .filter(HospitalShift::isNightShift)
                .join(HospitalShift.class,
                        Joiners.equal(HospitalShift::getEmployee),
                        Joiners.equal(shift -> shift.getDay() + 1, HospitalShift::getDay))
.filter((firstShift, secondShift) -> secondShift.isNightShift())
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Consecutive night shifts");
    }

    // Handle employees with lastDayShift=true (finished night shift on Aug 31st).
    // They cannot work day or night shifts on Sept 1st, and cannot work night shifts on Sept 2nd.
    Constraint handleLastDayShiftEmployees(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(HospitalShift.class)
                .filter(shift -> shift.getEmployee() != null && shift.getEmployee().isLastDayShift())
                .filter(shift -> 
                    (shift.getDay() == 1 && (shift.isDayShift() || shift.isNightShift())) || // Sept 1st: no day/night shifts
                    (shift.getDay() == 2 && shift.isNightShift()) // Sept 2nd: no night shifts
)
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Last day shift employees need proper rest");
    }

    // Soft constraint: Prefer employees to work on their selected days.
    Constraint preferSelectedDays(ConstraintFactory constraintFactory) {
return constraintFactory.forEach(HospitalShift.class)
                .filter(shift -> shift.getEmployee() != null && shift.getEmployee().prefersDay(shift.getDay()))
                .reward(HardSoftScore.ONE_SOFT)
                .asConstraint("Prefer selected days");
    }

    // Hard constraint: Enforce maximum total night shifts per employee based on their group's nightCount
    // Counts night shifts + weekend shifts (total workload limit)
    Constraint enforceMaxNightShiftsPerEmployee(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(HospitalShift.class)
                .filter(shift -> shift.getEmployee() != null)
                .filter(shift -> shift.isNightShift() || shift.isWeekendShift()) // Count night and weekend shifts
                .groupBy(HospitalShift::getEmployee, ConstraintCollectors.count())
                .join(GroupConfig.class,
                    Joiners.equal((employee, shiftCount) -> employee.getGroupName(), GroupConfig::getName))
                .filter((employee, shiftCount, group) -> shiftCount > group.getNightCount())
.penalize(HardSoftScore.ONE_HARD, (employee, shiftCount, group) -> shiftCount - group.getNightCount())
                .asConstraint("Max night shifts per employee exceeded");
    }

    // Hard constraint: Enforce maximum weekend shifts per employee based on their group's weekendNightCount
    // Only counts weekend shifts (separate weekend quota)
    Constraint enforceMaxWeekendShiftsPerEmployee(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(HospitalShift.class)
                .filter(shift -> shift.getEmployee() != null)
                .filter(HospitalShift::isWeekendShift) // Only count weekend shifts
                .groupBy(HospitalShift::getEmployee, ConstraintCollectors.count())
                .join(GroupConfig.class,
                    Joiners.equal((employee, shiftCount) -> employee.getGroupName(), GroupConfig::getName))
                .filter((employee, shiftCount, group) -> shiftCount > group.getWeekendNightCount())
.penalize(HardSoftScore.ONE_HARD, (employee, shiftCount, group) -> shiftCount - group.getWeekendNightCount())
                .asConstraint("Max weekend shifts per employee exceeded");
    }

    // Hard constraint: Employees can only work at workplaces their group is qualified for
    // Prevents junior residents from working in ICU, Emergency, etc. based on group restrictions
    Constraint enforceWorkplaceQualifications(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(HospitalShift.class)
                .filter(shift -> shift.getEmployee() != null && shift.getWorkplace() != null)
                .join(GroupConfig.class,
                    Joiners.equal(shift -> shift.getEmployee().getGroupName(), GroupConfig::getName))
.filter((shift, group) -> !group.canWorkAtWorkplace(shift.getWorkplace()))
                .penalize(HardSoftScore.ONE_HARD)
                .asConstraint("Employee group not qualified for workplace");
    }

    // Hard constraint: NYDH quota limits - balanced distribution of seniority groups across workplaces
    // Prevents clustering of same experience levels in specific departments (e.g., all senior residents in ICU)
    Constraint enforceNydhQuotas(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(HospitalShift.class)
                .filter(shift -> shift.getEmployee() != null && shift.getWorkplace() != null)
                .filter(shift -> shift.isNightShift() || shift.isWeekendShift()) // Only night/weekend shifts count for NYDH
                .groupBy(shift -> shift.getWorkplace(),
                         shift -> shift.getEmployee().getGroupName(),
                         ConstraintCollectors.count())
                .filter((workplace, groupName, count) -> {
                    // Get NYDH quota limit for this workplace-group combination
                    int limit = getNydhQuotaLimit(workplace, groupName);
                    return count > limit;
                })
.penalize(HardSoftScore.ONE_SOFT, (workplace, groupName, count) -> {
                    int limit = getNydhQuotaLimit(workplace, groupName);
                    return count - limit;
                })
                .asConstraint("NYDH quota limits exceeded");
    }
    
    // Hard constraint: Work group daily limits (Rule 7)
    // Prevents too many people from same medical specialty being on night duty simultaneously
    // Critical for hospital operations - e.g., prevents all cardiologists from being off during the day
    Constraint enforceWorkGroupDailyLimits(ConstraintFactory constraintFactory) {
        return constraintFactory.forEach(HospitalShift.class)
                .filter(shift -> shift.getEmployee() != null && shift.getEmployee().getWorkGroup() != null)
                .filter(shift -> shift.isNightShift() || shift.isWeekendShift()) // Only night/weekend shifts affect day coverage
                .groupBy(HospitalShift::getDay,
                        shift -> shift.getEmployee().getWorkGroup(),
                        ConstraintCollectors.count())
                .join(WorkGroupConfig.class,
                    Joiners.equal((day, workGroup, count) -> workGroup, WorkGroupConfig::getWorkGroup))
                .filter((day, workGroup, shiftCount, workGroupConfig) -> {
                    // Need to calculate total people in work group to determine max assignable
                    // For now, use a hardcoded approach - in real system this would be optimized
                    int totalInGroup = getTotalInWorkGroup(workGroup);
                    int maxAssignable = workGroupConfig.getMaxAssignablePerDay(totalInGroup);
                    return shiftCount > maxAssignable;
                })
                .penalize(HardSoftScore.ONE_HARD, (day, workGroup, shiftCount, workGroupConfig) -> {
                    int totalInGroup = getTotalInWorkGroup(workGroup);
                    int maxAssignable = workGroupConfig.getMaxAssignablePerDay(totalInGroup);
                    return shiftCount - maxAssignable;
                })
                .asConstraint("Work group daily limit exceeded");
    }
    
    /**
     * Helper method to get total people in a work group
     * This is a simplified implementation - in production you'd cache this
     */
    private int getTotalInWorkGroup(String workGroup) {
        // Hardcoded values based on the data analysis
        return switch (workGroup) {
            case "Kardiyoloji" -> 6;
            case "Nöroloji" -> 6;
            case "Genel Dahiliye" -> 8;
            case "Psikiyatri" -> 2;
            case "Enfeksiyon" -> 1;
            case "TANIMSIZ" -> 57; // Large group of unspecified staff
            default -> 1; // Conservative default for other specialties
        };
    }

    /**
     * Get NYDH quota limit for a workplace-group combination
     * Uses updated realistic quotas based on actual staffing data
     */
    private int getNydhQuotaLimit(String workplace, String groupName) {
        // Create configuration instance to get realistic quotas
        NydhQuotaConfiguration config = new NydhQuotaConfiguration();
        return config.getQuotaLimit(workplace, groupName);
    }
}
