package org.acme.employeescheduling.domain;

import java.util.List;
import java.util.Objects;

/**
 * Configuration for employee groups (y11, y31p, etc.)
 * Contains workplace permissions and shift limits
 */
public class GroupConfig {
    private String name; // e.g., "y11", "y31p"
    private int count; // Number of employees in this group
    private int nightCount; // Max night shifts per employee
    private int weekendNightCount; // Max weekend shifts per employee
    private List<String> workPlaces; // Workplaces this group can work at
    
    public GroupConfig() {
    }

    public GroupConfig(String name, int count, int nightCount, int weekendNightCount, List<String> workPlaces) {
        this.name = name;
        this.count = count;
        this.nightCount = nightCount;
        this.weekendNightCount = weekendNightCount;
        this.workPlaces = workPlaces;
    }

    public boolean canWorkAtWorkplace(String workplace) {
        return workPlaces != null && workPlaces.contains(workplace);
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getNightCount() {
        return nightCount;
    }

    public void setNightCount(int nightCount) {
        this.nightCount = nightCount;
    }

    public int getWeekendNightCount() {
        return weekendNightCount;
    }

    public void setWeekendNightCount(int weekendNightCount) {
        this.weekendNightCount = weekendNightCount;
    }

    public List<String> getWorkPlaces() {
        return workPlaces;
    }

    public void setWorkPlaces(List<String> workPlaces) {
        this.workPlaces = workPlaces;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GroupConfig that)) return false;
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
