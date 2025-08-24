package org.acme.employeescheduling.config;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;
import java.util.HashMap;

/**
 * NYDH (Turkish Hospital Seniority Distribution) Quota Configuration
 * 
 * Manages quotas for different seniority groups (y11, y31p, etc.) 
 * across various hospital workplaces (Gastroenteroloji, Acil, etc.)
 * 
 * This ensures balanced experience distribution across all departments
 * preventing clustering of same experience levels in specific workplaces.
 */
@ApplicationScoped
public class NydhQuotaConfiguration {
    
    /**
     * Workplace quotas mapping
     * Key: workplace name (e.g., "Gastroenteroloji", "Acil")
     * Value: WorkplaceQuota containing group limits
     */
    private Map<String, WorkplaceQuota> workplaces;
    
    /**
     * Initialize with default NYDH quotas
     */
    public NydhQuotaConfiguration() {
        initializeDefaultQuotas();
    }

    /**
     * Get quota limit for a specific workplace and group combination
     * 
     * @param workPlace The workplace name (e.g., "Gastroenteroloji")
     * @param groupName The seniority group (e.g., "y11", "o52") 
     * @return The quota limit, or 0 if not configured
     */
    public int getQuotaLimit(String workPlace, String groupName) {
        if (workplaces == null) {
            return 999; // No limit if not configured
        }
        
        WorkplaceQuota quota = workplaces.get(workPlace);
        if (quota == null || quota.groupLimits == null) {
            return 999; // No limit if not configured
        }
        
        return quota.groupLimits.getOrDefault(groupName, 999);
    }
    
    /**
     * Check if NYDH quotas are configured for a workplace
     */
    public boolean hasQuotasFor(String workPlace) {
        return workplaces != null && 
               workplaces.containsKey(workPlace) && 
               workplaces.get(workPlace) != null &&
               workplaces.get(workPlace).groupLimits != null;
    }

    /**
     * Quota configuration for a specific workplace
     */
    public static class WorkplaceQuota {
        /**
         * Group limits mapping
         * Key: seniority group name (e.g., "y11", "y31", "o52", "c43")
         * Value: maximum number of assignments allowed for this group
         */
        public Map<String, Integer> groupLimits;
        
        /**
         * Whether this workplace should enforce NYDH quotas
         * Default: true
         */
        public boolean enabled = true;
        
        /**
         * Description of this workplace's quota rules
         */
        public String description;

        // Getters and setters
        public Map<String, Integer> getGroupLimits() {
            return groupLimits;
        }

        public void setGroupLimits(Map<String, Integer> groupLimits) {
            this.groupLimits = groupLimits;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public Map<String, WorkplaceQuota> getWorkplaces() {
        return workplaces;
    }

    public void setWorkplaces(Map<String, WorkplaceQuota> workplaces) {
        this.workplaces = workplaces;
    }
    
/**
     * Initialize realistic NYDH quotas based on actual staffing data
     * 
     * Actual staffing: y11=3, y31p=16, y31=8, y32p=7, y32=3, y61=1,
     *                 o42=2, o52p=15, o52=6, c52=9, c43=10 (total: 80 employees)
     * 
     * Previous quotas were too restrictive causing -10820 hard score violations.
     * New quotas allow more flexibility while maintaining medical safety standards.
     */
    private void initializeDefaultQuotas() {
        workplaces = new HashMap<>();
        
        // Acil - Emergency Department (high-demand, needs experienced staff)
        WorkplaceQuota acilQuota = new WorkplaceQuota();
        acilQuota.enabled = true;
        acilQuota.description = "Emergency Department - High-demand, needs experienced staff";
        acilQuota.groupLimits = new HashMap<>();
acilQuota.groupLimits.put("y11", 2);    // 2/3 junior residents  
        acilQuota.groupLimits.put("y31p", 6);   // 6/16 senior residents
        acilQuota.groupLimits.put("y31", 4);    // 4/8 residents
        acilQuota.groupLimits.put("y32p", 3);   // 3/7 senior residents
        acilQuota.groupLimits.put("y32", 2);    // 2/3 residents
        acilQuota.groupLimits.put("y61", 1);    // 1/1 (all available)
        acilQuota.groupLimits.put("o42", 2);    // 2/2 instructors
        acilQuota.groupLimits.put("o52p", 6);   // 6/15 senior instructors
        acilQuota.groupLimits.put("o52", 3);    // 3/6 instructors
        acilQuota.groupLimits.put("c52", 4);    // 4/9 consultants
        acilQuota.groupLimits.put("c43", 4);    // 4/10 senior consultants
        workplaces.put("Acil", acilQuota);
        
        // Yoğun Bakım - ICU (critical care, needs senior staff)
        WorkplaceQuota icuQuota = new WorkplaceQuota();
        icuQuota.enabled = true;
        icuQuota.description = "Intensive Care Unit - Critical care, needs senior staff";
        icuQuota.groupLimits = new HashMap<>();
icuQuota.groupLimits.put("y11", 1);    // 1/3 junior residents (limited)
        icuQuota.groupLimits.put("y31p", 4);   // 4/16 senior residents
        icuQuota.groupLimits.put("y31", 3);    // 3/8 residents
        icuQuota.groupLimits.put("y32p", 2);   // 2/7 senior residents
        icuQuota.groupLimits.put("y32", 1);    // 1/3 residents
        icuQuota.groupLimits.put("y61", 0);    // No y61 in ICU (policy)
        icuQuota.groupLimits.put("o42", 1);    // 1/2 instructors
        icuQuota.groupLimits.put("o52p", 4);   // 4/15 senior instructors
        icuQuota.groupLimits.put("o52", 2);    // 2/6 instructors
        icuQuota.groupLimits.put("c52", 3);    // 3/9 consultants
        icuQuota.groupLimits.put("c43", 3);    // 3/10 senior consultants
        workplaces.put("Yoğun Bakım", icuQuota);
        
        // Gastroenteroloji
        WorkplaceQuota gastroQuota = new WorkplaceQuota();
        gastroQuota.enabled = true;
        gastroQuota.description = "Gastroenterology Department";
        gastroQuota.groupLimits = new HashMap<>();
gastroQuota.groupLimits.put("y11", 1);    // 1/3 junior residents
        gastroQuota.groupLimits.put("y31p", 4);   // 4/16 senior residents
        gastroQuota.groupLimits.put("y31", 3);    // 3/8 residents
        gastroQuota.groupLimits.put("y32p", 2);   // 2/7 senior residents
        gastroQuota.groupLimits.put("y32", 1);    // 1/3 residents
        gastroQuota.groupLimits.put("y61", 0);    // No y61 in specialty
        gastroQuota.groupLimits.put("o42", 1);    // 1/2 instructors
        gastroQuota.groupLimits.put("o52p", 4);   // 4/15 senior instructors
        gastroQuota.groupLimits.put("o52", 2);    // 2/6 instructors
        gastroQuota.groupLimits.put("c52", 3);    // 3/9 consultants
        gastroQuota.groupLimits.put("c43", 3);    // 3/10 senior consultants
        workplaces.put("Gastroenteroloji", gastroQuota);
        
        // Add other workplaces with basic quotas
        addBasicWorkplaceQuota("Genel Dahiliye", "General Internal Medicine - Main department");
        addBasicWorkplaceQuota("Nefroloji", "Nephrology Department");
        addBasicWorkplaceQuota("Onkoloji", "Oncology Department");  
        addBasicWorkplaceQuota("Hematoloji", "Hematology Department");
        addBasicWorkplaceQuota("KİT", "General Internal Medicine");
        addBasicWorkplaceQuota("Palyatif", "Palliative Care");
    }
    
    /**
     * Helper method to add generous workplace quotas for general departments
     * These departments get higher quotas for scheduling flexibility
     */
    private void addBasicWorkplaceQuota(String workplaceName, String description) {
        WorkplaceQuota quota = new WorkplaceQuota();
        quota.enabled = true;
        quota.description = description;
        quota.groupLimits = new HashMap<>();
        quota.groupLimits.put("y11", 2);    // 2/3 junior residents
        quota.groupLimits.put("y31p", 5);   // 5/16 senior residents  
        quota.groupLimits.put("y31", 3);    // 3/8 residents
        quota.groupLimits.put("y32p", 3);   // 3/7 senior residents
        quota.groupLimits.put("y32", 2);    // 2/3 residents
        quota.groupLimits.put("y61", 1);    // 1/1 (all available)
        quota.groupLimits.put("o42", 2);    // 2/2 instructors
        quota.groupLimits.put("o52p", 5);   // 5/15 senior instructors
        quota.groupLimits.put("o52", 3);    // 3/6 instructors  
        quota.groupLimits.put("c52", 3);    // 3/9 consultants
        quota.groupLimits.put("c43", 4);    // 4/10 senior consultants
        workplaces.put(workplaceName, quota);
    }
}