package org.acme.employeescheduling.domain;

public enum ShiftType {
    DAY_SHIFT,      // Weekday 9am-5pm
    NIGHT_SHIFT,    // Weekday 5pm-9am (next day)
    WEEKEND_SHIFT   // Weekend 9am-9am (24 hours)
}
