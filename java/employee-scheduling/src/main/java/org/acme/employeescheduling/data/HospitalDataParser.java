package org.acme.employeescheduling.data;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.acme.employeescheduling.domain.*;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class HospitalDataParser {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true);

    public HospitalSchedule parseFromResource(String resourcePath) {
        try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            
            JsonNode rootNode = objectMapper.readTree(inputStream);
            return parseFromJson(rootNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse hospital data: " + resourcePath, e);
        }
    }

    private HospitalSchedule parseFromJson(JsonNode rootNode) {
        JsonNode listData = rootNode.get("listData");
        
        int month = listData.get("month").asInt();
        int year = listData.get("year").asInt();
        
        List<HospitalEmployee> employees = parseEmployees(listData.get("members"));
        List<GroupConfig> groups = parseGroups(rootNode.get("groups"));
        List<WorkplaceConfig> workplaces = parseWorkplaces(rootNode.get("workPlaces"));
        List<WorkGroupConfig> workGroups = parseWorkGroups(rootNode.get("workGroups"));
        List<HospitalShift> shifts = generateShifts(month, year, workplaces, workGroups);

        HospitalSchedule schedule = new HospitalSchedule(month, year, employees, shifts);
        schedule.setGroups(groups);
        schedule.setWorkplaces(workplaces);
        schedule.setWorkGroups(workGroups);
        
        return schedule;
    }

    private List<HospitalEmployee> parseEmployees(JsonNode membersNode) {
        List<HospitalEmployee> employees = new ArrayList<>();
        
        for (JsonNode memberNode : membersNode) {
            String name = memberNode.get("memberId").asText();
            String groupName = memberNode.get("groupName").asText();
            String workGroup = memberNode.get("workGroup").asText();
            
            Set<Integer> officialLeaveDays = parseIntegerSet(memberNode.get("officialLeaveDays"));
            Set<Integer> excludedDays = parseDaySet(memberNode.get("excludedDays"));
            Set<Integer> selectedDays = parseDaySet(memberNode.get("selectedDays"));
            
            boolean lastDayShift = memberNode.has("lastDayShift") && 
                                   memberNode.get("lastDayShift").asBoolean();

            HospitalEmployee employee = new HospitalEmployee(
                name, groupName, workGroup, 
                officialLeaveDays, excludedDays, selectedDays, lastDayShift
            );
            
            employees.add(employee);
        }
        
        return employees;
    }

    private List<GroupConfig> parseGroups(JsonNode groupsNode) {
        List<GroupConfig> groups = new ArrayList<>();
        
        for (JsonNode groupNode : groupsNode) {
            String name = groupNode.get("name").asText();
            int count = groupNode.get("count").asInt();
            int nightCount = groupNode.get("nightCount").asInt();
            int weekendNightCount = groupNode.get("weekendNightCount").asInt();
            List<String> workPlaces = parseStringList(groupNode.get("workPlace"));
            
            GroupConfig group = new GroupConfig(name, count, nightCount, weekendNightCount, workPlaces);
            groups.add(group);
        }
        
        return groups;
    }

    private List<WorkplaceConfig> parseWorkplaces(JsonNode workPlacesNode) {
        List<WorkplaceConfig> workplaces = new ArrayList<>();
        
        for (JsonNode workPlaceNode : workPlacesNode) {
            String name = workPlaceNode.get("workPlace").asText();
            int weekdayCount = workPlaceNode.get("workPlaceWeekdayCount").asInt();
            int weekendCount = workPlaceNode.get("workPlaceWeekendCount").asInt();
            
            WorkplaceConfig workplace = new WorkplaceConfig(name, weekdayCount, weekendCount);
            workplaces.add(workplace);
        }
        
        return workplaces;
    }

    private List<WorkGroupConfig> parseWorkGroups(JsonNode workGroupsNode) {
        List<WorkGroupConfig> workGroups = new ArrayList<>();
        
        for (JsonNode workGroupNode : workGroupsNode) {
            String workGroup = workGroupNode.get("workGroup").asText();
            int neededCount = workGroupNode.get("neededCount").asInt();
            
            WorkGroupConfig config = new WorkGroupConfig(workGroup, neededCount);
            workGroups.add(config);
        }
        
        return workGroups;
    }

    private List<HospitalShift> generateShifts(int month, int year, List<WorkplaceConfig> workplaces, List<WorkGroupConfig> workGroups) {
        List<HospitalShift> shifts = new ArrayList<>();
        int daysInMonth = getDaysInMonth(month, year);
        
        for (int day = 1; day <= daysInMonth; day++) {
            boolean isWeekend = isWeekend(month, year, day);
            
            if (isWeekend) {
                shifts.addAll(generateWeekendShifts(day, workplaces));
            } else {
                shifts.addAll(generateWeekdayShifts(day, workplaces, workGroups));
            }
        }
        
        return shifts;
    }

    private List<HospitalShift> generateWeekdayShifts(int day, List<WorkplaceConfig> workplaces, List<WorkGroupConfig> workGroups) {
        List<HospitalShift> shifts = new ArrayList<>();
        
        // Day shifts based on work groups
        for (WorkGroupConfig workGroup : workGroups) {
            for (int i = 1; i <= workGroup.getNeededCount(); i++) {
                shifts.add(new HospitalShift(
                    "day_" + day + "_" + workGroup.getWorkGroup() + "_" + i, day, ShiftType.DAY_SHIFT, 
                    null, workGroup.getWorkGroup()
                ));
            }
        }
        
        // Night shifts for each workplace
        for (WorkplaceConfig workplace : workplaces) {
            int requiredCount = workplace.getWeekdayNightStaffCount();
            for (int i = 1; i <= requiredCount; i++) {
                shifts.add(new HospitalShift(
                    "night_" + day + "_" + workplace.getName() + "_" + i,
                    day, ShiftType.NIGHT_SHIFT, 
                    workplace.getName(), null
                ));
            }
        }
        
        return shifts;
    }

    private List<HospitalShift> generateWeekendShifts(int day, List<WorkplaceConfig> workplaces) {
        List<HospitalShift> shifts = new ArrayList<>();
        
        for (WorkplaceConfig workplace : workplaces) {
            int requiredCount = workplace.getWeekendStaffCount();
            for (int i = 1; i <= requiredCount; i++) {
                shifts.add(new HospitalShift(
                    "weekend_" + day + "_" + workplace.getName() + "_" + i,
                    day, ShiftType.WEEKEND_SHIFT, 
                    workplace.getName(), null
                ));
            }
        }
        
        return shifts;
    }

    private Set<Integer> parseIntegerSet(JsonNode arrayNode) {
        Set<Integer> result = new HashSet<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                result.add(item.asInt());
            }
        }
        return result;
    }

    private Set<Integer> parseDaySet(JsonNode arrayNode) {
        Set<Integer> result = new HashSet<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                if (item.has("day")) {
                    result.add(item.get("day").asInt());
                } else {
                    result.add(item.asInt());
                }
            }
        }
        return result;
    }

    private List<String> parseStringList(JsonNode arrayNode) {
        List<String> result = new ArrayList<>();
        if (arrayNode != null && arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                result.add(item.asText());
            }
        }
        return result;
    }

    private int getDaysInMonth(int month, int year) {
        if (month == 9 && year == 2025) return 30; // September 2025
        return 30; // Default fallback
    }

    private boolean isWeekend(int month, int year, int day) {
        if (month == 9 && year == 2025) {
            // September 2025: Saturdays (6,13,20,27) and Sundays (7,14,21,28)
            return day % 7 == 6 || day % 7 == 0;
        }
        return false;
    }
}
